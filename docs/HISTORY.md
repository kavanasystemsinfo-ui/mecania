# HISTORY.md

Evolución del proyecto Mecania y decisiones descartadas.

## 2026-09-04 (Fase 2: procesamiento y embeddings — tras remediación)

- **Extracción de texto**: `TextExtractor` con implementaciones PDFBox (PDF), POI (DOCX) y plain UTF-8 (TXT), selección por `TextExtractorFactory`.
- **Chunking**: `SlidingWindowChunker` (512 chars con overlap 64, ventana deslizante determinista).
- **Embeddings**: `EmbeddingService` + `OpenRouterEmbeddingService` (modelo por defecto `text-embedding-3-small`). La API key se resuelve en el constructor con una única fuente: property `mecania.embedding.api-key` o fallback env var `OPENROUTER_API_KEY` (se eliminó la doble fuente vía `System.getProperty`, que era una trampa).
- **Procesamiento async**: `DocumentoProcessor` (pool `mecania-proc-` 2/4/100) orquesta extracción→chunking→embeddings→persistencia de fragmentos.
- **Carrera async/commit corregida**: `DocumentoService.subirDocumento` ya NO llama al método async dentro de la transacción. Publica `DocumentoSubidoEvent`; `DocumentoSubidoEventListener` con `@TransactionalEventListener(AFTER_COMMIT)` dispara el procesamiento solo tras el commit (la versión previa podía dejar documentos en PROCESANDO para siempre).
- **Atomicidad real**: `DocumentoProcesadorTransaccional` (bean separado, evita self-invocation) con `@Transactional(rollbackFor = Exception.class)` — un fallo a mitad revierte TODOS los fragmentos (los tests de integración lo verifican con fallo en el 2º fragmento).
- **Seguridad**: `AlmacenamientoDiscoLocal` sanitiza nombres de archivo y rutas contra path traversal (`..`); lectura/borrado rechazan rutas que escapen del baseDir.
- **Endpoints de testing eliminados**: `/marcar-listo` y `/marcar-error` ya no existen (estado gestionado por el procesador; cualquiera podía marcar documentos como LISTO con `permitAll`).
- **Tests**: la suite pasó de 26 a **68 tests** (62 unitarios + 6 integración) en `mvn verify`. El commit anterior de fase 2 afirmaba "19 new tests all green" con un IT que no compilaba; tras la auditoría, la cifra es real y verificada ejecutando la suite.
- **ADR 004**: actualizado para reflejar el código real (RestTemplate, evento AFTER_COMMIT, rollbackFor).

## 2026-09-04 (Fase 1: subida de manuales)
- **Endpoints de documentos implementados**: `POST /api/vehiculos/{id}/documentos` (subida), `GET /api/vehiculos/{id}/documentos` (listado), `GET /api/vehiculos/{id}/documentos/{docId}/download` (descarga). Aceptan PDF, TXT y DOCX hasta 10 MB.
- **Entidades reactivadas**: `Documento` (con relación a `Vehiculo`) y `Fragmento` (con relación a `Documento`). La columna `embedding` queda como `double[]` por ahora; se migrará a tipo `vector` de pgvector cuando llegue la fase RAG.
- **Abstracción de almacenamiento**: interfaz `AlmacenamientoArchivos` + impl `AlmacenamientoDiscoLocal` con tests reales de filesystem (sin mocks).
- **Frontend actualizado**: pestaña "Documentos" en el modal de edición de vehículo con form de subida, lista de documentos subidos y botón de descarga.
- **Tests**: de 12 a 26 tests verdes (10 nuevos en `DocumentoServiceTest`, 4 nuevos en `AlmacenamientoDiscoLocalTest`).
- **ADR 003**: almacenamiento en disco local ahora, Supabase Storage o S3 cuando se despliegue a producción real.

## 2026-09-04 (Reestructuración para portfolio)
- **Inicio de reestructuración para portfolio**: Se decidió mejorar la presencia pública del repositorio (README, licencia, seguridad, plantillas de issue) siguiendo el estándar de Kavana para piezas de portafolio.
- **Limpieza de entidades huérfanas**: Se eliminaron las clases `Documento`, `Fragmento` y `Conversacion` que estaban definidas como entidades JPA pero sin uso en el código (ni en servicios, ni en controllers, ni en tests). Su presencia generaba tablas vacías y claves foráneas rotas en cada arranque.
- **ADR 002: Seguridad consciente en MVP**: Se documentó la decisión de mantener `permitAll()` y CSRF desactivado durante la fase de validación de features core, como paso previo a implementar autenticación real (JWT) en una iteración posterior.

## 2026-09-03 (estado previo)
- Proyecto inicial con CRUD de vehículos (`/api/vehiculos`) completo y testeado (12 tests unitarios/integración verdes).
- Stack: Java 21 + Spring Boot 3.4 + PostgreSQL 16 + extensión pgvector (en docker-compose).
- Frontend Bootstrap funcional (no commiteado inicialmente).
- ADR 001: Stack tecnológico ya existente.
- Entidades `Documento`, `Fragmento`, `Conversacion` presentes pero sin uso.
- Seguridad: `permitAll()` en `SecurityConfig.java`.
