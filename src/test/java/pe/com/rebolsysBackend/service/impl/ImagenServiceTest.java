package pe.com.rebolsysBackend.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import pe.com.rebolsysBackend.exception.ImagenInvalidaException;
import pe.com.rebolsysBackend.exception.RecursoNoEncontradoException;
import pe.com.rebolsysBackend.repository.HabitacionRepository;
import pe.com.rebolsysBackend.repository.ProductoRepository;

class ImagenServiceTest {
  @TempDir Path directorio;
  HabitacionRepository habitaciones;
  ProductoRepository productos;
  ImagenService servicio;

  @BeforeEach void preparar() {
    habitaciones = mock(HabitacionRepository.class); productos = mock(ProductoRepository.class);
    when(habitaciones.existsById(7L)).thenReturn(true);
    when(productos.existsById(9L)).thenReturn(true);
    servicio = new ImagenService(directorio.toString(), habitaciones, productos);
  }

  private byte[] imagen(String formato, int ancho, int alto) throws Exception {
    var salida = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB), formato, salida);
    return salida.toByteArray();
  }

  @Test void guardaElArchivoEnLaInstanciaYSigueDisponibleTrasRecrearElServicio() throws Exception {
    var foto = new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", imagen("jpeg", 3, 2));
    assertThat(servicio.guardar("habitaciones", 7L, foto)).isEqualTo("/api/v1/imagenes/habitaciones/7.png");
    assertThat(ImageIO.read(directorio.resolve("habitaciones/7.png").toFile()).getWidth()).isEqualTo(3);
    var reiniciado = new ImagenService(directorio.toString(), habitaciones, productos);
    assertThat(reiniciado.ruta("habitaciones", 7L)).isEqualTo("/api/v1/imagenes/habitaciones/7.png");
    verify(habitaciones).existsById(7L);
    verifyNoMoreInteractions(habitaciones, productos);
  }

  @Test void ignoraElNombreOriginalYNoPermiteEscaparDelDirectorio() throws Exception {
    var foto = new MockMultipartFile("archivo", "../../fuera.png", "image/png", imagen("png", 2, 2));
    servicio.guardar("productos", 9L, foto);
    assertThat(directorio.resolve("productos/9.png")).isRegularFile();
    assertThatThrownBy(() -> servicio.guardar("../fuera", 9L, foto)).isInstanceOf(ImagenInvalidaException.class);
  }

  @Test void rechazaTextoDisfrazadoDeImagenYConservaLaImagenAnterior() throws Exception {
    servicio.guardar("habitaciones", 7L, new MockMultipartFile("archivo", "foto.png", "image/png", imagen("png", 2, 2)));
    var anterior = Files.readAllBytes(directorio.resolve("habitaciones/7.png"));
    var falsa = new MockMultipartFile("archivo", "foto.png", "image/png", "<html>no es imagen</html>".getBytes());
    assertThatThrownBy(() -> servicio.guardar("habitaciones", 7L, falsa)).isInstanceOf(ImagenInvalidaException.class);
    assertThat(Files.readAllBytes(directorio.resolve("habitaciones/7.png"))).isEqualTo(anterior);
  }

  @Test void rechazaExcesoDeTamanoYDimensiones() throws Exception {
    var grande = new MockMultipartFile("archivo", "foto.png", "image/png", new byte[(int) ImagenService.MAX_BYTES + 1]);
    assertThatThrownBy(() -> servicio.guardar("habitaciones", 7L, grande)).isInstanceOf(ImagenInvalidaException.class);
    var ancha = new MockMultipartFile("archivo", "foto.png", "image/png", imagen("png", 4097, 1));
    assertThatThrownBy(() -> servicio.guardar("habitaciones", 7L, ancha)).isInstanceOf(ImagenInvalidaException.class);
  }

  @Test void noGuardaImagenesParaRegistrosInexistentes() throws Exception {
    var foto = new MockMultipartFile("archivo", "foto.png", "image/png", imagen("png", 2, 2));
    assertThatThrownBy(() -> servicio.guardar("habitaciones", 999L, foto)).isInstanceOf(RecursoNoEncontradoException.class);
  }

  @Test void quitarLaFotoEsIdempotenteYNoEliminaElRegistro() throws Exception {
    servicio.guardar("productos", 9L, new MockMultipartFile("archivo", "foto.png", "image/png", imagen("png", 2, 2)));
    servicio.eliminar("productos", 9L); servicio.eliminar("productos", 9L);
    assertThat(servicio.ruta("productos", 9L)).isNull();
    verify(productos, times(3)).existsById(9L);
    verifyNoMoreInteractions(productos, habitaciones);
  }
}
