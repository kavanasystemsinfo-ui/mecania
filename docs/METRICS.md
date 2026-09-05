# METRICS.md

Qué cubren los tests, no solo cuántos.

## Resumen

Suite ejecutada con `mvn verify` (2026-09-04, tras remediación de fase 2):
**68 tests en 12 suites — todos verdes** (62 unitarios en `mvn test` + 6 de
integración con failsafe). Las cifras de este archivo salen de ejecutar la
suite, no de contar `@Test` con grep.

### Capa de controlador (VehiculoControllerTest)
- **5 tests**: endpoints REST bajo condiciones variadas (listado total y por usuarioId, obtención por ID, creación exitosa y conflicto por duplicado, actualización, eliminación).

### Capa de controlador — integración (VehiculoControllerIT)
- **3 tests**: flujo real con contexto Spring (CRUD de vehículos sobre BD H2, validación de errores HTTP).

### Capa de servicio (VehiculoServiceTest)
- **7 tests**: lógica de servicio sin layer HTTP (findAll/findByUsuarioId, findById con/ sin existencia, create con duplicado y éxito, update, delete).

### Capa de servicio de documentos (DocumentoServiceTest)
- **8 tests**: subida (PDF válido, TXT válido, extensión no soportada, archivo demasiado grande, vehículo inexistente), listado, pertenencia del documento al vehículo.
- **Clave**: los tests de subida verifican que se publica el evento `DocumentoSubidoEvent` con el id correcto (el procesamiento async NO se llama directo — se dispara por listener AFTER_COMMIT, ver ADR 004).

### Capa de dominio — chunking (SlidingWindowChunkerTest)
- **11 tests**: null/vacío, texto corto, tamaño exacto, overlap correcto, determinismo, tamaño máximo, validación de constructor (overlap >= chunkSize, negativo, chunkSize <= 0), overlap cero.

### Capa de dominio — embeddings (EmbeddingTest)
- **5 tests**: construcción + inmutabilidad defensiva, null/vacío rechazados, formato pgvector, equals por contenido.

### Capa de infraestructura — extracción (3 suites)
- **PdfBoxTextExtractorTest (3)**: PDF extraído, PDF corrupto → ExtractionException, contenido vacío.
- **PlainTextExtractorTest (5)**: TXT UTF-8, vacío, binario, null.
- **TextExtractorFactoryTest (4)**: selección por tipo, tipo sin extractor → error.

### Capa de infraestructura — embeddings (OpenRouterEmbeddingServiceTest)
- **7 tests**: llamada correcta con MockRestServiceServer (URL, método, header Bearer, content-type), texto vacío/null, error 500, respuesta vacía, dimensión configurada, dimensión diferente avisa pero no falla.

### Capa de infraestructura — almacenamiento (AlmacenamientoDiscoLocalTest)
- **7 tests**: guardar/leer/eliminar reales con tempdir, subdirectorio vacío, y **3 tests de seguridad**: nombre de archivo con `../` se sanitiza y no escapa del baseDir, lectura y borrado con rutas traviesas se rechazan (path traversal).

### Capa de infraestructura — procesamiento (DocumentoProcessorIT)
- **3 tests de integración**: extrae→chunckea→persiste fragmentos, error sin API key marca documento ERROR sin fragmentos (espera determinista por polling, no Thread.sleep), y el caso clave de **fallo a mitad** (embedding falla en el 2º fragmento → rollback real → CERO fragmentos parciales).

## Qué NO está cubierto actualmente (y por qué)

- **Capa de persistencia directa**: no hay tests con EntityManager/native query porque la capa de repository abstrae esa interacción; se añadirá en fase 4 con pgvector.
- **Escenarios de concurrencia**: no se testa aún condiciones de carrera bajo carga alta; se asumirá en fases posteriores si el dominio lo requiere.
- **Integración con pgvector / RAG**: aún no implementado; se añadirá en los tests de esas features.
- **Endpoints de autenticación**: no existen todavía; se cubrirán cuando se implemente login/registro.
- **Testcontainers para PostgreSQL**: las dependencias están declaradas pero no se usan todavía (las pruebas actuales son unitarias con mocks o H2).

## Cómo leer este archivo

Este métrico no pretende ser una métrica de cobertura de línea, sino una
**declaración de intención**: qué aspectos del dominio están verificados por la
suite. Cada vez que se añada una feature, actualizar esta sección con lo que
sus tests cubren (y ejecutar la suite antes de tocar las cifras).