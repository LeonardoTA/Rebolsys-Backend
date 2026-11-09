package pe.com.rebolsysBackend.service.impl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pe.com.rebolsysBackend.exception.ImagenInvalidaException;
import pe.com.rebolsysBackend.exception.RecursoNoEncontradoException;
import pe.com.rebolsysBackend.repository.HabitacionRepository;
import pe.com.rebolsysBackend.repository.ProductoRepository;

/** Los archivos viven en la instancia; su relación se determina por tipo e identificador. */
@Service
public class ImagenService {
  public static final long MAX_BYTES = 5L * 1024 * 1024;
  private final Path directorio;
  private final HabitacionRepository habitaciones;
  private final ProductoRepository productos;

  public ImagenService(@Value("${app.imagenes.directorio:./.local/imagenes}") String directorio,
      HabitacionRepository habitaciones, ProductoRepository productos) {
    this.directorio = Path.of(directorio).toAbsolutePath().normalize();
    this.habitaciones = habitaciones;
    this.productos = productos;
  }

  public String guardar(String tipo, Long id, MultipartFile archivo) {
    comprobarRegistro(tipo, id);
    if (archivo == null || archivo.isEmpty() || archivo.getSize() > MAX_BYTES)
      throw new ImagenInvalidaException("Selecciona una imagen PNG o JPG de hasta 5 MB");
    BufferedImage imagen;
    try (var entrada = new MemoryCacheImageInputStream(archivo.getInputStream())) {
      var lectores = ImageIO.getImageReaders(entrada);
      if (!lectores.hasNext()) throw new ImagenInvalidaException("El archivo no es una imagen PNG o JPG válida");
      var lector = lectores.next();
      try {
        if (!Set.of("png", "jpeg", "jpg").contains(lector.getFormatName().toLowerCase(java.util.Locale.ROOT)))
          throw new ImagenInvalidaException("Solo se admiten imágenes PNG y JPG");
        lector.setInput(entrada, true, true);
        int ancho = lector.getWidth(0), alto = lector.getHeight(0);
        if (ancho < 1 || alto < 1 || ancho > 4096 || alto > 4096 || (long) ancho * alto > 16_000_000)
          throw new ImagenInvalidaException("La imagen debe tener como máximo 4096 píxeles por lado y 16 megapíxeles");
        imagen = lector.read(0);
      } finally { lector.dispose(); }
    } catch (IOException e) {
      throw new ImagenInvalidaException("No se pudo leer la imagen. Selecciona un PNG o JPG válido");
    }
    Path destino = archivo(tipo, id), temporal = null;
    try {
      Files.createDirectories(destino.getParent());
      temporal = Files.createTempFile(destino.getParent(), "imagen-", ".tmp");
      if (!ImageIO.write(imagen, "png", temporal.toFile())) throw new IOException("Codificador PNG no disponible");
      if (Files.size(temporal) > 20L * 1024 * 1024)
        throw new ImagenInvalidaException("La imagen ocupa demasiado espacio. Elige una fotografía más pequeña");
      try {
        Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
      }
      return rutaPublica(tipo, id);
    } catch (IOException e) {
      throw new UncheckedIOException("No se pudo guardar la imagen en la instancia", e);
    } finally {
      if (temporal != null) try { Files.deleteIfExists(temporal); } catch (IOException ignored) { }
    }
  }

  public Path obtener(String tipo, Long id) {
    comprobarRegistro(tipo, id);
    var archivo = archivo(tipo, id);
    if (!Files.isRegularFile(archivo, LinkOption.NOFOLLOW_LINKS))
      throw new RecursoNoEncontradoException("Este registro no tiene una imagen guardada");
    return archivo;
  }

  public void eliminar(String tipo, Long id) {
    comprobarRegistro(tipo, id);
    try { Files.deleteIfExists(archivo(tipo, id)); }
    catch (IOException e) { throw new UncheckedIOException("No se pudo quitar la imagen de la instancia", e); }
  }

  public String ruta(String tipo, Long id) {
    if (id == null) return null;
    return Files.isRegularFile(archivo(tipo, id), LinkOption.NOFOLLOW_LINKS) ? rutaPublica(tipo, id) : null;
  }

  private String rutaPublica(String tipo, Long id) { return "/api/v1/imagenes/" + tipo + "/" + id + ".png"; }

  private Path archivo(String tipo, Long id) {
    if (!Set.of("habitaciones", "productos").contains(tipo) || id == null || id <= 0)
      throw new ImagenInvalidaException("El registro de la imagen no es válido");
    return directorio.resolve(tipo).resolve(id + ".png");
  }

  private void comprobarRegistro(String tipo, Long id) {
    archivo(tipo, id);
    boolean existe = "habitaciones".equals(tipo) ? habitaciones.existsById(id) : productos.existsById(id);
    if (!existe) throw new RecursoNoEncontradoException("El registro no existe");
  }
}
