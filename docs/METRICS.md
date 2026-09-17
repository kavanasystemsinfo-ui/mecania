# METRICS.md

Qué cubren los tests, no solo cuántos.

## Resumen

Suite ejecutada con `mvn verify` (2026-09-17):
**211 tests en 30 suites — todos verdes** (178 unitarios en `mvn test` + 33 de
integración con failsafe). Las cifras de este archivo salen de ejecutar la
suite, no de contar `@Test` con grep.

### Capa de controlador (VehiculoControllerTest)
- **5 tests**: endpoints REST del vehículo (listado del usuario autenticado, obtención por ID, creación exitosa, validación fallida → 400 con `fields`, y 404 por ID inexistente).

### Capa de controlador — integración (VehiculoControllerIT, BusquedaManualesControllerIT, AlertaControllerIT)
- **VehiculoControllerIT (3)**: flujo real con contexto Spring (CRUD de vehículos sobre BD H2, validación de errores HTTP).
- **AlertaControllerIT (2)**: flujo crear/listar/actualizar/eliminar (201/200/404/204) y revisión de vencidas que devuelve la alerta vencida y la deja desactivada.
- **BusquedaManualesControllerIT (11)**: contrato HTTP de la Fase 3 con el buscador y el descargador mockeados: candidatos devueltos sin descargar nada, consulta libre traducida a "Toyota Corolla 2018 manual cambio de aceite", 503 con `buscador_no_disponible` cuando el buscador bloquea, 404 si el vehículo no existe, 201 al importar, 400 con URL vacía (validación) y con URL no http, 415 con tipo no soportado, 413 con archivo demasiado grande, 404 en importación de vehículo inexistente y **200 con `yaExistia: true` al reimportar sin duplicar documentos**.
- **AuthControllerIT (6)**: contrato HTTP de autenticación con la seguridad REAL activa (perfil `test-auth`): registro 201 con token, registro duplicado 409, login 200 con token, login con contraseña incorrecta 401, endpoint protegido sin token 401 y con token 200.
- **MultiTenenciaIT (2)**: un usuario autenticado no ve (404) ni puede borrar (404) vehículos ajenos, y el listado de un usuario sin vehículos propios está vacío.
- **HealthControllerIT (1)**: `/health` devuelve 200 `UP` sin autenticación (hace `SELECT 1` contra H2).
- **HealthReadyIT (3)**: `/health/ready` devuelve 200 `UP` con las tres comprobaciones en verde (base de datos, clave de embeddings y almacenamiento escribible) y 503 `DOWN` en cuanto falla la clave de embeddings o el almacenamiento: el servicio no puede declararse listo con la IA muerta.

### Contrato de errores (GlobalExceptionHandlerTest)
- **4 tests**: archivo demasiado grande → **413** con código propio `archivo_demasiado_grande`; subida cortada por el límite del servidor de aplicaciones → **413** con el límite en MB y sin el nombre de la clase de la excepción; ruta inexistente → **404** `recurso_no_encontrado` (antes 500); error no previsto → **500** `error_interno` con mensaje genérico, sin filtrar el nombre de la clase ni el mensaje interno.

### Capa de servicio (VehiculoServiceTest)
- **7 tests**: lógica de servicio sin layer HTTP acotada al usuario (findByUsuarioId, findById con/sin existencia, create con duplicado y éxito, update, delete).

### Capa de servicio de documentos (DocumentoServiceTest)
- **9 tests**: subida (PDF válido, TXT válido, extensión no soportada, vehículo inexistente, **un byte por encima del límite → `ArchivoDemasiadoGrandeException` y exactamente en el límite → aceptado**), listado, pertenencia del documento al vehículo. El límite no está en el código: sale de `mecania.upload.max-bytes`, así que los tests de frontera construyen el servicio con un límite pequeño y no reservan 25 MB de heap.
- **Clave**: los tests de subida verifican que se publica el evento `DocumentoSubidoEvent` con el id correcto (el procesamiento async NO se llama directo — se dispara por listener AFTER_COMMIT, ver ADR 004).

### Capa de servicio de búsqueda asistida (BusquedaManualesServiceTest)
- **13 tests**: construcción de la consulta (marca, modelo, año, "manual" y consulta libre, ignorando espacios), candidatos devueltos tal cual, vehículo inexistente → error sin llamar al buscador, fallo del buscador propagado sin disfrazar, importación que descarga/guarda/crea documento y publica el evento, normalización por `trim` de la URL pegada, **reimportación que devuelve el existente sin descargar ni publicar evento**, descarga fallida que no crea documento ni escribe archivo, y fallo de escritura envuelto con mensaje explícito.

### Capa de servicio de chat RAG (ChatManualesServiceTest)
- **4 tests**: respuesta con los fragmentos relevantes devolviendo las fuentes; sin fragmentos por encima del umbral → "sin base" SIN llamar al LLM; descarte de fragmentos por debajo de `mecania.chat.similitud-minima` (el prompt no incluye el irrelevante); propagación del fallo del LLM.

### Capa de seguridad — JWT (JwtServiceTest)
- **6 tests**: token válido devuelve la identidad; token manipulado, de otro secreto o caducado → inválido; secreto demasiado corto o vacío → rechazado en construcción.

### Capa de servicio de autenticación (AuthServiceTest)
- **5 tests**: registro hashea la contraseña y devuelve token; email duplicado → excepción; login correcto devuelve token; contraseña incorrecta y email inexistente → excepción.

### Capa de servicio de alertas (AlertaServiceTest)
- **10 tests**: crear (ok y vehículo inexistente), listar, actualizar (ok y no encontrada → `AlertaNotFoundException`), eliminar (ok y no encontrada), vencidas (única desactivada, mensual avanza, sin vencidas vacío).

### Capa de dominio — chunking (SlidingWindowChunkerTest)
- **11 tests**: null/vacío, texto corto, tamaño exacto, overlap correcto, determinismo, tamaño máximo, validación de constructor (overlap >= chunkSize, negativo, chunkSize <= 0), overlap cero.

### Capa de dominio — embeddings (EmbeddingTest)
- **5 tests**: construcción + inmutabilidad defensiva, null/vacío rechazados, formato pgvector, equals por contenido.

### Capa de dominio — alertas (RevisorAlertasTest)
- **7 tests**: lista vacía, futura no devuelta, inactiva no devuelta, única vencida devuelta y desactivada, mensual avanza un mes, anual salta ocurrencias pasadas, mensual muy vencida salta hasta el futuro.

### Capa de infraestructura — extracción (3 suites)
- **PdfBoxTextExtractorTest (3)**: PDF extraído, PDF corrupto → ExtractionException, contenido vacío.
- **PlainTextExtractorTest (5)**: TXT UTF-8, vacío, binario, null.
- **TextExtractorFactoryTest (4)**: selección por tipo, tipo sin extractor → error.

### Capa de infraestructura — búsqueda (ParserResultadosDdgTest, DuckDuckGoBuscadorManualesTest)
- **ParserResultadosDdgTest (11)**: HTML fijo con la estructura real de DDG → título/URL/fragmento/fuente; resolución del redirect `uddg` (nunca se devuelve un enlace de duckduckgo.com); marcado de PDFs; limpieza de etiquetas y entidades; bloques sin título o sin URL ignorados; lista vacía sin resultados, con HTML vacío y con null; deduplicación por URL; tope de resultados; enlaces absolutos sin redirección; y **filtrado de los anuncios** (`/y.js?ad_domain=...`) detectado contra DDG real.
- **DuckDuckGoBuscadorManualesTest (7)**: contra un servidor local que imita a DDG → candidatos parseados, lista vacía cuando de verdad no hay resultados, consulta codificada y cabeceras de navegador enviadas (sin UA de navegador DDG devuelve su anti-bot), **challenge anti-bot → `BusquedaException` con "bloqueado"** (nunca lista vacía), error HTTP 500 explícito, sin conexión → error claro y tope de resultados respetado.

### Capa de infraestructura — descarga (HttpDescargadorUrlTest, ValidadorHostsPublicosTest)
- **HttpDescargadorUrlTest (20)**: PDF con nombre/tipo/bytes correctos, nombre del `Content-Disposition`, tipo por extensión cuando el `Content-Type` es genérico, TXT, tipo no soportado → 415, por encima del tope de bytes → 413, HTTP 404 → error de red con el código en el mensaje, **HTTP 403 con la pista de que el origen bloquea descargas automatizadas (caso real de w3.org)**, URLs inválidas o con esquema no http (`file://`, `ftp://`, `javascript:`, vacío, texto suelto, null), host privado rechazado con el validador real, redirección seguida con URL final correcta, **cada salto de la redirección pasa por el validador**, exceso de redirecciones, sin conexión → error claro, y `Content-Disposition` que intenta escapar de la carpeta reducido a nombre base.
- **ValidadorHostsPublicosTest (17)**: 12 casos de host rechazado (loopback, `localhost`, IPv6 loopback, 0.0.0.0, 10.x, 172.16-31.x, 192.168.x, 169.254.169.254 de metadatos, `metadata.google.internal`), 3 hosts públicos aceptados, host sin nombre rechazado y el validador permisivo de desarrollo aceptando loopback.

### Capa de infraestructura — embeddings (OpenRouterEmbeddingServiceTest)
- **7 tests**: llamada correcta con MockRestServiceServer (URL, método, header Bearer, content-type), texto vacío/null, error 500, respuesta vacía, dimensión configurada, dimensión diferente avisa pero no falla.

### Capa de infraestructura — almacenamiento (AlmacenamientoDiscoLocalTest)
- **18 tests**: guardar/leer/eliminar reales con tempdir, subdirectorio vacío, **3 tests de seguridad** (nombre con `../` se sanitiza y no escapa del baseDir; lectura y borrado con rutas traviesas se rechazan), hardening de nombres especiales (`/`, `.`, `..`, byte NUL) y rutas que colapsan sobre la raíz, más **5 tests de la vía `guardarArchivo(byte[])`** que usa la importación desde internet: escritura real, nombre travieso saneado, nombres especiales, contenido vacío rechazado y subdirectorio que escapa rechazado.

### Capa de infraestructura — procesamiento (DocumentoProcessorIT)
- **5 tests de integración** (sobre H2, perfil `test`): extrae→chunckea→persiste fragmentos, error sin API key marca documento ERROR sin fragmentos (espera determinista por polling, no Thread.sleep), **fallo a mitad** (embedding falla en el 2º fragmento → rollback real → CERO fragmentos), **se guarda un vector por fragmento** (id y embedding correctos) y **fallo del almacén de vectores revierte la transacción** (CERO fragmentos).

## Qué NO está cubierto actualmente (y por qué)

- **Búsqueda real contra DuckDuckGo en CI**: los tests usan un servidor local con HTML fijo; la verificación contra el servicio real es manual (smoke test), porque depender de un tercero haría el CI no determinista. Tampoco se prueba el caso de que DDG cambie su HTML: se detectaría como "sin resultados", y el camino alternativo (pegar la URL) sigue disponible.
- **Descarga real desde internet en CI**: cubierta con servidor local; los hosts públicos se validan con tests unitarios del validador.
- **Persistencia y búsqueda pgvector en la suite**: la query SQL real (`<=>`, `PGobject`, `CREATE EXTENSION`) NO se cubre en la suite porque H2 no tiene pgvector. El puerto `RepositorioVectores` se mockea en los tests (lógica del chat y rollback), y el SQL real se verifica con un smoke test contra el PostgreSQL de desarrollo.
- **Escenarios de concurrencia**: no se testa aún condiciones de carrera bajo carga alta; se asumirá en fases posteriores si el dominio lo requiere.
- **Testcontainers para PostgreSQL**: las dependencias están declaradas pero no se usan todavía; las pruebas actuales son unitarias con mocks o H2.

## Cómo leer este archivo

Este métrico no pretende ser una métrica de cobertura de línea, sino una
**declaración de intención**: qué aspectos del dominio están verificados por la
suite. Cada vez que se añada una feature, actualizar esta sección con lo que
sus tests cubren (y ejecutar la suite antes de tocar las cifras).
