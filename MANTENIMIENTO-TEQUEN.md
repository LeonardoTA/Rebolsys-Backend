# Aporte de mantenimiento — James Tequen

El backend de iteración 1 se conserva como base en `main`. La rama `Dev-Tequen`
contiene la integración del mantenimiento de habitaciones (CU_21), inmobiliario
(CU_22) y productos (CU_23) con el frontend independiente.

## Cambios del backend

- Las habitaciones se retiran de nuevas reservas mediante el estado existente
  `MANTENIMIENTO`, conservando sus registros e historial.
- Se utilizan los contratos existentes para asignación, traslado, conservación,
  baja e historial de mobiliario y para catálogo y ajustes de stock de productos.
- Se añade carga, consulta y eliminación de fotos de habitaciones y productos.
  Los archivos se guardan en `./.local/imagenes` o en `REBOLSYS_IMAGENES_DIR`, sin
  binarios ni columnas nuevas en Oracle. Las respuestas calculan su `imagenRuta`.
- Las imágenes PNG/JPG se validan y normalizan antes de reemplazar una fotografía.
  Un archivo inválido conserva la imagen anterior. Ver [API-IMAGENES.md](API-IMAGENES.md).

La carpeta de fotografías debe persistir y respaldarse con la instancia. En un
despliegue con contenedores debe montarse en un volumen persistente.

## Configuración local y arranque

Ejecutar `.\iniciar-backend.ps1` desde este repositorio. El puerto predeterminado
es 8085; puede cambiarse con `-Puerto`. Se requiere Java 21 o superior y Oracle
accesible. El script utiliza temporales y cachés de Maven en la unidad del proyecto.

La conexión privada de esta máquina permanece en
`.local/application-local-private.properties`, excluida de Git. El esquema
`PropuestaHospedajeRB` fue preparado con autorización del usuario en `oracle-local`.
En otra máquina se debe configurar una conexión propia y preparar el esquema
correspondiente; clonar el repositorio o iniciar el backend no crea esquemas ni
carga datos automáticamente. También se admiten `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`.

## Verificación

La suite completa contra Oracle real registró **126 casos: 110 aprobados,
16 opcionales omitidos y ningún fallo ni error**. El modelo coincidió con el DDL:
47 tablas y 288 columnas. Las seis pruebas nuevas de imágenes cubren persistencia,
rutas seguras, validación de archivos y límites, y eliminación independiente del registro.

Las comprobaciones HTTP cubrieron registro, consulta, edición y retiro de
habitaciones; asignación, conservación, baja e historial de mobiliario; y alta,
ajuste, edición y retiro de productos. También se verificaron reintentos sin
duplicación de movimientos y carga, rechazo y eliminación de imágenes.

Para repetir las pruebas de integración con una conexión privada ya configurada:

```powershell
$env:RUN_ORACLE_TESTS = 'true'
.\mvnw.cmd -B -ntp package
```

## Integración pendiente

El login y la autorización por rol corresponden a la integración con el equipo:
gerente para habitaciones e inmobiliario y recepcionista para productos. El
responsable de un movimiento no sustituye una sesión autenticada. La descripción
de habitaciones queda para otra iteración. Los endpoints de imágenes deben recibir
los mismos permisos al integrar la autenticación.

Repositorios de entrega: [backend](https://github.com/LeonardoTA/Rebolsys-Backend)
y [frontend](https://github.com/LeonardoTA/Rebolsys-Frontend).
