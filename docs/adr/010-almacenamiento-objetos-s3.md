# ADR 010: Almacenamiento de manuales en un servicio de objetos compatible con S3 (Cloudflare R2)

**Estado:** Implementado
**Fecha:** 2026-09-17
**Sustituye a:** [ADR-003](003-almacenamiento-manuales-disco-local.md) para el entorno de producción (el disco local sigue siendo el modo de desarrollo)

## Contexto

El ADR-003 dejó el almacenamiento detrás de la interfaz `AlmacenamientoArchivos` y anunció que "cuando se decida desplegar a producción real, se sustituye por Supabase/S3 implementando la misma interfaz". Ese momento llegó el 2026-09-17, al poner la aplicación en producción en Render con base de datos en Neon.

El problema no es teórico: **el sistema de archivos de un contenedor es efímero**. En cada redespliegue se borran los manuales mientras la base de datos conserva documentos, fragmentos, vectores y conversaciones que los citan. El resultado es peor que un error limpio: descargas en 404 y respuestas del chat RAG apoyadas en una fuente documental que ya no existe. Lo detectó la auditoría de Fase 0 y quedó como el hueco de producción de mayor impacto para el usuario.

Restricción de coste: el proyecto es una pieza de portafolio, no genera ingresos, así que la solución tiene que caber en un nivel gratuito real.

## Decisión

Implementamos **`AlmacenamientoS3`**, una implementación de `AlmacenamientoArchivos` sobre un servicio de objetos compatible con S3, usando el **AWS SDK v2** (2.39.6) y **Cloudflare R2** como proveedor en producción.

- La implementación se elige con la propiedad `mecania.storage.tipo` (`local` por defecto, `s3` en producción). Los dos beans existen en el código y solo uno se registra en el contexto: si se registran los dos, el arranque falla con "required a single bean, but 2 were found" (pasó, y hay un test que lo cubre).
- El valor que se guarda en base de datos sigue siendo la misma ruta relativa (`vehiculos/82/documentos/manual-grande.docx`): **es la clave del objeto**. Cambiar de backend no cambia el modelo de datos ni los registros existentes.
- La descarga se hace **en flujo** (`leerArchivoStream`), no cargando el objeto entero en memoria: el contenedor de producción tiene 384 MB de heap y un manual puede pesar 25 MB.
- El almacén expone `disponible()` y `DiagnosticoEntorno` lo usa en `/health/ready`: con el bucket caído el servicio no se declara listo. Antes esa comprobación miraba un directorio local que en producción no significa nada.
- La sanitización de nombres y subdirectorios se centraliza y **las dos implementaciones se someten al mismo test de contrato** para que las reglas no se separen con el tiempo.

## Alternativas consideradas

### Alternativa 1: Disco con volumen persistente (plan de pago de Render)
- **Pros**: cero código nuevo; el mismo `AlmacenamientoDiscoLocal` sirve.
- **Contras**: 7-8 $/mes para siempre para guardar unos megas; el disco va atado al servicio, así que no hay acceso desde fuera ni CDN; y sigue siendo un problema de backup manual (la BD se respalda sola, los ficheros no).
- **Descartada por**: coste recurrente para una pieza de portafolio, sin ventaja técnica.

### Alternativa 2: Supabase Storage
- **Pros**: nivel gratuito de 1 GB, interfaz sencilla, el ADR-003 lo nombraba.
- **Contras**: 1 GB es menos que los 10 GB de R2, mete un segundo proveedor con su propio modelo de autenticación, y es otra pieza de las que dependen de un panel (el proyecto ya tiene contrato con un proveedor de IA y con Neon).
- **Descartada por**: menos capacidad y más acoplamiento sin ganancia funcional.

### Alternativa 3: Amazon S3
- **Pros**: el estándar, el más documentado.
- **Contras**: la capa gratuita son 5 GB durante 12 meses y después factura por almacenamiento y por salida de datos; la gestión de IAM y políticas es desproporcionada para este caso.
- **Descartada por**: coste y complejidad administrativa frente a un compatible-S3 sin salida de datos.

### Alternativa 4: MinIO autogestionado en el VPS
- **Pros**: coste cero (el VPS ya está pagado), control total, ningún proveedor nuevo.
- **Contras**: hay que operarlo, respaldarlo y asegurarlo; el VPS ya aloja la preproducción, el Postgres de desarrollo y Hermes, y su disco está al 74 %. Un almacén de objetos autogestionado sin backup propio es exactamente la fragilidad que este cambio quiere quitar.
- **Descartada para producción**; se usa **para los tests de verdad** (ver más abajo), que es su mejor papel aquí.

### Alternativa 5: Guardar los binarios en PostgreSQL (`BYTEA` / `OID` / `large objects`)
- **Pros**: un solo sistema, backup y transaccionalidad ya resueltos.
- **Contras**: hincha la base de datos con contenido que no se consulta, encarece cada volcado y backup, y obliga a que el tráfico de ficheros pase por la capa de datos. En Neon, además, el tamaño cuenta para el nivel gratuito.
- **Descartada por**: mezclar almacenamiento de objetos con datos relacionales a este tamaño no aporta nada.

## Consecuencias

**Positivas**
- Los manuales sobreviven a los redespliegues: verificado matando el proceso, comprobando que el objeto sigue en el bucket y descargando el mismo fichero con **hash idéntico** después de reiniciar.
- El mismo valor de base de datos sirve para los dos backends: migrar de uno a otro es cambiar el tipo y copiar objetos.
- La descarga en flujo evita el pico de memoria de un manual grande.
- El bucket es la puerta a lo que viene después sin tocar el dominio: CDN delante, URLs firmadas, ciclo de vida.

**Negativas y riesgos asumidos**
- Una dependencia nueva (AWS SDK v2) y un juego de credenciales más que rotar. Se admiten a cambio de no perder datos.
- La descarga paga una ida y vuelta de red que antes era un `read` local. Con el objeto en el mismo continente y en flujo, el coste es aceptable.
- **Los manuales subidos antes de este cambio, alojados en el disco efímero de Render, se pierden en el siguiente despliegue.** No se migran (son datos de prueba): la base de datos conserva sus filas y esas descargas darán 404, que es exactamente el comportamiento que este ADR viene a eliminar para lo nuevo.
- **Nada borra objetos todavía**: se implementó `eliminarArchivo` pero hoy no hay endpoint de borrado de documentos, y borrar un vehículo no borra sus objetos del bucket. Quedan objetos huérfanos si se usa ese endpoint existente. Es un pendiente explícito, con su propio ticket, no una sorpresa.

## Verificación

- **Tests unitarios con el cliente S3 mockeado** (14): construcción de la clave, sanitización de nombres y subdirectorios, contenido vacío, mapeo de errores del proveedor, flujo de lectura, borrado y disponibilidad.
- **Test de contrato** (3) con las dos implementaciones a la vez: mismo nombre → misma ruta relativa; el mismo intento de salirse del almacén falla en los dos.
- **Test de selección de bean** (2): sin configuración manda el disco; con `storage.tipo=s3` manda el almacén de objetos y el contexto arranca.
- **Verificación real contra MinIO** en el VPS (`/root/scripts/mecania_s3_check.sh`): subida de un DOCX de 5,6 MB → objeto visible en el bucket → nada escrito en el disco de la aplicación → procesamiento y chat RAG citando el fragmento → proceso matado y relanzado → descarga con **sha256 idéntico al original**.
