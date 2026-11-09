package pe.com.rebolsysBackend.controller;

import java.nio.file.Files;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.com.rebolsysBackend.service.impl.ImagenService;

@RestController
@RequestMapping("/api/v1/imagenes")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Imágenes de mantenimiento")
public class ImagenController {
  private final ImagenService imagenes;
  public record ImagenRespuesta(String ruta) { }

  @PostMapping(value = "/{tipo}/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ImagenRespuesta subir(@PathVariable String tipo, @PathVariable Long id,
      @RequestPart("archivo") MultipartFile archivo) {
    return new ImagenRespuesta(imagenes.guardar(tipo, id, archivo));
  }

  @GetMapping(value = "/{tipo}/{id}.png", produces = MediaType.IMAGE_PNG_VALUE)
  public ResponseEntity<Resource> ver(@PathVariable String tipo, @PathVariable Long id) throws IOException {
    var archivo = imagenes.obtener(tipo, id);
    return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).cacheControl(CacheControl.noStore())
        .contentLength(Files.size(archivo)).header("X-Content-Type-Options", "nosniff")
        .body(new FileSystemResource(archivo));
  }

  @DeleteMapping("/{tipo}/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void quitar(@PathVariable String tipo, @PathVariable Long id) { imagenes.eliminar(tipo, id); }
}
