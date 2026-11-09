# Imágenes de mantenimiento

Los archivos se guardan en la instancia y no en Oracle. La carpeta se configura mediante `REBOLSYS_IMAGENES_DIR` o `app.imagenes.directorio`; el valor predeterminado es `./.local/imagenes`, relativo al directorio del backend. Mantener esta carpeta en almacenamiento persistente.

| Operación | Ruta | Datos / resultado |
|---|---|---|
| Subir o reemplazar | `POST /api/v1/imagenes/{tipo}/{id}` | `multipart/form-data`, campo `archivo`; devuelve `{ "ruta": "/api/v1/imagenes/habitaciones/1.png" }` |
| Consultar | `GET /api/v1/imagenes/{tipo}/{id}.png` | Imagen PNG; 404 cuando no existe el registro o la foto |
| Quitar foto | `DELETE /api/v1/imagenes/{tipo}/{id}` | 204; repetir la eliminación no produce error si el registro existe |

`tipo` admite `habitaciones` o `productos`; `id` debe corresponder a un registro guardado. El nombre del archivo del cliente no se utiliza para construir rutas. Se validan el contenido real, tamaño máximo de 5 MB, máximo de 4096 píxeles por lado y 16 megapíxeles. Solo se admiten PNG y JPG, que se normalizan a PNG. Un reemplazo inválido conserva la foto anterior.

Las respuestas de configuración de habitaciones añaden `imagenRuta`, calculada al consultar el archivo. En productos se usa la ruta de la foto de instancia si existe y se conserva la compatibilidad con la ruta de imagen anterior. No hay migraciones de base de datos.

La carga de fotos es independiente de la edición del registro. Puede realizarse desde la edición o el detalle de una habitación o producto ya guardado. El endpoint devuelve 400 para imágenes inválidas y 413 para exceso de tamaño de carga.

Estos endpoints siguen la etapa actual sin autenticación. Al integrar el login del equipo, aplicarles los permisos de mantenimiento correspondientes y derivar el responsable desde la sesión.
