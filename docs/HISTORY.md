# HISTORY.md

Evolución del proyecto Mecania y decisiones descartadas.

## 2026-09-17 (Auditoría de Fase 0 y primer endurecimiento)

- **Auditoría real del repo** contra código y producción: 5 afirmaciones de la documentación que el código desmiente y 10 huecos para producción. Informe en `auditoria-mecania/2026-09-17-fase0-auditoria-real.md`.
- **Producción real**: Render (Docker, Frankfurt, plan gratuito) + Neon (PostgreSQL 16 con pgvector) + `mecania.kavanasystems.com` con HTTPS. Verificado end-to-end: registro → vehículo → subida de manual → `LISTO` con embeddings → chat RAG citando el fragmento correcto. Arranque persistente en el VPS por systemd (`mecania.service`): la caída del 8080 era un proceso en primer plano que moría al cerrar la sesión.
- **Límite de subida corregido**: el real era **1 MB** (valor por defecto de Spring Boot, sin configurar) mientras la documentación prometía 10 MB, y un fichero de 2 MB devolvía **500**. Ahora **25 MB** declarados en `spring.servlet.multipart.*` y en `mecania.upload.max-bytes`, con el servicio validando el mismo número. `ArchivoDemasiadoGrandeException` y `MaxUploadSizeExceededException` mapeadas a **413** con el límite en el mensaje: las dos vías devuelven el mismo contrato.
- **Contrato de errores**: ruta inexistente → **404** `recurso_no_encontrado` (antes 500 con el nombre de la excepción) y el manejador genérico ya no devuelve `clase: mensaje` al cliente: el detalle va al log y el cliente recibe un mensaje genérico. Filtraba la estructura interna en cada error, incluida `/swagger-ui.html`.
- **Secreto JWT sin valor por defecto**: `mecania.jwt.secret=${JWT_SECRET:}`. Antes el repositorio traía un secreto de desarrollo público y un despliegue sin la variable arrancaba sin quejarse, firmando tokens con un valor conocido.
- **`/health/ready`**: readiness que comprueba base de datos, clave de embeddings y almacenamiento escribible (503 si falla alguna). Motivo concreto: el despliegue de hoy tenía `/health` en `UP` con el pipeline de embeddings muerto porque el contenedor no veía la clave.
- **Tests**: 203 → **211** (178 unitarios + 33 de integración): +4 `GlobalExceptionHandlerTest`, +3 `HealthReadyIT`, +1 de frontera en `DocumentoServiceTest`.
- **Almacenamiento de manuales en un servicio de objetos (ADR-010)**: el disco de un contenedor es efímero, así que cada redespliegue borraba los manuales mientras la BD seguía guardando documentos, fragmentos y vectores que los citaban (descargas en 404 y respuestas RAG sobre una fuente desaparecida). Implementado `AlmacenamientoS3` sobre el AWS SDK v2 detrás de la misma interfaz `AlmacenamientoArchivos` que dejó el ADR-003, seleccionable con `mecania.storage.tipo` (`local` en desarrollo, `s3` en producción con Cloudflare R2). El valor de BD sigue siendo la misma ruta relativa, que ahora es la clave del objeto. La descarga pasa a servirse **en flujo** (un manual de 25 MB no tiene por qué pasar entero por un heap de 384 MB) y `/health/ready` comprueba el bucket en lugar de un directorio local. Verificado contra un S3 real (MinIO en el VPS): DOCX de 5,6 MB subido, visible en el bucket, **nada escrito en el disco de la aplicación**, procesado y chateado, y tras **matar y relanzar el proceso** la descarga devuelve un sha256 idéntico al original.
- **Hueco de seguridad encontrado al tocar la descarga (IDOR)**: `GET /api/vehiculos/{id}/documentos/{docId}/download` no comprobaba que el vehículo fuera del usuario autenticado, a diferencia de la subida y el listado. Bastaba con adivinar ids para descargar manuales de otra cuenta; ahora devuelve 404 y hay un test de multi-tenencia que lo cubre.
- **Dos fallos de arranque cazados por los tests nuevos**: con los dos almacenes anotados sin condición, arrancar en modo S3 fallaba con *"required a single bean, but 2 were found"*, y el adaptador con dos constructores necesitaba `@Autowired` explícito en el de producción. `SeleccionDeAlmacenamientoIT` levanta el contexto en los dos modos para que no vuelva a pasar.
- **Tests**: 211 → **232** (195 unitarios + 37 de integración): +14 `AlmacenamientoS3Test`, +3 `AlmacenamientoContratoTest`, +2 `SeleccionDeAlmacenamientoIT`, +1 de descarga ajena en `MultiTenenciaIT`, +1 `SecurityHeadersIT` (CSP).
- **Cierre de endurecimiento (mismo día)** — commit `32ed2f1`:
    - **Cabeceras HTTP de seguridad** en `SecurityConfig`: CSP (`default-src 'self'`, jsdelivr e inline permitidos para no romper la UI, `frame-ancestors 'none'`), `X-Content-Type-Options: nosniff` y `X-Frame-Options: DENY`. El token JWT vive en `localStorage`; sin CSP un XSS lo exfiltra sin esfuerzo. Cubierto por `SecurityHeadersIT` para que un refactor no lo pierda.
    - **Perfil `prod`**: `spring.jpa.show-sql=false` y el SQL de Hibernate a nivel INFO; en Render se activa con `SPRING_PROFILES_ACTIVE=prod`. Se deja de volcar emails y contenido de manuales en los logs de producción (eran el volcado del fichero base).
    - **Swagger de verdad (springdoc)**: `/swagger-ui.html` y `/v3/api-docs` sirven la API documentada, con scheme Bearer para probar desde la propia UI. El badge de OpenAPI del README vuelve a ser cierto.
    - **ADR-009** (plataformas) escrito e indexado; índice de `DECISIONS.md` con los 9 enlaces rotos corregidos y la fila ADR-009/010.
    - **README**: retiradas las dos afirmaciones falsas (seguridad abierta `permitAll`; "no hay registro/login"); fecha al día.
    - **docker-compose**: imagen de Postgres de desarrollo fijada a PG15 (el `latest` era una etiqueta móvil); producción sigue en Neon PG16.
    - **Contenedor sin root + HEALTHCHECK** en el `Dockerfile`, y `src/test` fuera del contexto de build (builds más rápidos).
    - **Operación**: vigilante externo de `/health/ready` cada 10 min (cron D5, aguanta el arranque en frío de 30-90 s para no dar falsos positivos y avisa por Telegram con cooldown), backup diario de la BD a R2 (`backups/`, rotación a 7) y **auto-deploy** activado en `main`.

## 2026-09-16 (Fase 7: despliegue y monitoreo)

- **CI**: workflow `.github/workflows/ci.yml` que ejecuta `mvn verify` en cada push a `main` (y en PRs).
- **Health check**: endpoint `/health` (200 `UP` / 503 `DOWN`) que hace `SELECT 1` contra la BD y no requiere auth (liveness para Render/Fly). Sin actuator: se evitó la dependencia (no estaba en el repo offline).
- **DocumentoProcessorIT a H2**: antes corría contra el Postgres de desarrollo (destructivo y bloqueaba el CI sin Postgres); ahora usa `@ActiveProfiles("test")` y `PgVectorSchemaInitializer` ya saltaba H2.
- **Docker**: `Dockerfile` multi-etapa (Maven → JRE 21) + `spring-boot-maven-plugin` (jar ejecutable) + `.dockerignore` + `render.yaml`.
- **Config por entorno**: `DATABASE_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` por env vars con defaults de desarrollo.
- **Logging**: `logback-spring.xml` con rotación (10 MB, 7 días, tope 100 MB).
- **Docs**: `docs/DEPLOY.md` con la guía Render + Neon.
- **Tests**: 203 tests (173 unitarios + 30 de integración), +1 por `HealthControllerIT`.
- **Pendiente**: el deploy real a Render + Neon (requiere las cuentas del titular).

## 2026-09-16 (Fase 6: autenticación y multi-tenencia)

- **JWT HS256 con jjwt 0.11.5**: `POST /api/auth/register` (201) y `/api/auth/login` (200) devuelven token; `JwtAuthenticationFilter` valida y extrae `uid`; contraseñas con BCrypt.
- **Protección**: `/api/auth/**` abierto, resto de `/api/**` exige token (401 con JSON). `mecania.auth.enabled=false` desactiva la seguridad en el perfil de test de lógica de negocio.
- **Multi-tenencia**: `VehiculoRequest` ya no lleva `usuarioId` (el dueño sale del token); `VehiculoRepository.findByIdAndUsuarioId/existsByIdAndUsuarioId`; los controllers anidados (documentos, manuales, chat, alertas) verifican dueño con `SecurityUtils.usuarioIdActual()`. Acceso a un vehículo ajeno → 404.
- **Tests**: la suite pasó de 183 a **202 tests** (173 unitarios + 29 de integración). Nuevos: `JwtServiceTest` (6), `AuthServiceTest` (5), `AuthControllerIT` (6), `MultiTenenciaIT` (2).
- **ADR 008**: documenta JWT vs sesión vs OAuth2, la gestión del secreto (desarrollo vs entorno) y la multi-tenencia.
- **UI de login**: overlay Entrar/Crear cuenta con estética pastel, sesión en localStorage, token en todas las llamadas vía `apiFetch` y botón Salir. Verificado con smoke test real (register 201, crear vehículo 201 con el id del token, listar solo propios, login incorrecto 401).

## 2026-09-16 (Fase 5: alertas de mantenimiento)

- **Entidad `Alerta`** con `TipoAlerta` (ITV/ACEITE/FRENOS/CUSTOM) y `Repetitividad` (UNICA/MENSUAL/ANUAL), asociada a `Vehiculo`.
- **Lógica de vencimiento pura** en `RevisorAlertas`: una alerta única vencida se devuelve y se desactiva; una repetitiva avanza su fecha saltando las ocurrencias pasadas. Testeable sin base de datos (7 tests).
- **CRUD anidado** bajo `/api/vehiculos/{id}/alertas` (consistente con documentos/manuales/chat), con validación y `AlertaNotFoundException` → 404.
- **Revisión de vencidas** en `GET .../alertas/vencidas`: ejecuta el revisor, persiste las mutaciones y devuelve las vencidas. Notificación por log de consola (webhook futuro).
- **UI**: pestaña "Alertas" en el modal del vehículo (formulario, listado con eliminar y botón "Revisar vencidas").
- **Tests**: la suite pasó de 164 a **183 tests** (162 unitarios + 21 de integración). 7 de `RevisorAlertasTest`, 10 de `AlertaServiceTest` y 2 de `AlertaControllerIT`.
- **Verificación real**: smoke test (alerta ITV vencida + ACEITE futura → `vencidas` devuelve solo la ITV y la desactiva, la futura queda intacta).
- **ADR 007**: documenta el anidamiento, la lógica de repetitividad y el porqué de la revisión manual (sin `@Scheduled`).

## 2026-09-16 (Fase 4: chat RAG por vehículo)

- **Persistencia de embeddings por fin**: hasta ahora el vector se calculaba y se descartaba. Nueva tabla auxiliar `fragmento_embeddings` gestionada por JDBC nativo (`PgVectorRepositorioVectores` + `PGobject` tipo `vector`), porque Hibernate-core no mapea el tipo `vector`. La crea `PgVectorSchemaInitializer` (ApplicationRunner idempotente, solo sobre PostgreSQL) con `CREATE EXTENSION vector` + tabla.
- **Búsqueda por similitud restringida al vehículo**: coseno con el operador `<=>` de pgvector y `WHERE d.vehiculo_id = ?`. Es estructuralmente imposible cruzar manuales de vehículos distintos.
- **Chat con honestidad**: `ChatManualesService` vectoriza la pregunta, recupera top-K por encima de un umbral de similitud (0,2 configurable) y, si no hay fragmentos relevantes, responde "sin base" SIN llamar al LLM. `OpenRouterLlmService` (chat/completions, gpt-4o-mini) responde solo con los fragmentos; la API devuelve la respuesta + las fuentes que la sostienen.
- **Atomicidad**: el vector se guarda en la misma transacción que el fragmento; un fallo del almacén revierte todo.
- **Endpoint**: `POST /api/vehiculos/{vehiculoId}/chat` con `{"pregunta": "..."}`.
- **Tests**: la suite pasó de 158 a **164 tests** (145 unitarios + 19 de integración). 4 tests nuevos de `ChatManualesService` + 2 de `DocumentoProcessorIT` (persistencia de un vector por fragmento y rollback por fallo del almacén de vectores).
- **Verificación real**: smoke test completo contra PostgreSQL y OpenRouter (subir TXT → 5 fragmentos con vector dim 1536 → pregunta sobre el aceite → respuesta "15.000 km o 12 meses" con 4 fuentes y su similitud coseno).
- **ADR 006**: documenta la elección de JDBC nativo frente a custom Hibernate type o Flyway.

## 2026-09-16 (Fase 3: búsqueda asistida de manuales)

- **Cambio de diseño respecto al plan inicial**: la Fase 3 se planteó como "Asistente Tavily". Se descarta esa API (y AIsa/Perplexity) por coste: consumen saldo del titular del proyecto y no hay presupuesto de operación en una pieza de portfolio. Sustituida por scraping del HTML público de DuckDuckGo (`html.duckduckgo.com/html/`) con Jsoup, dependencia nueva declarada en `pom.xml`.
- **Búsqueda que PROPONE, no descarga**: `GET /api/vehiculos/{id}/manuales/candidatos[?q=]` devuelve título, URL real, fragmento, host y si apunta a PDF. El usuario decide; nada se descarga por buscar.
- **URL real, no la del intermediario**: DDG envuelve los enlaces en `/l/?uddg=<url codificada>`; `ParserResultadosDdg` resuelve el parámetro para que el usuario vea y acepte la fuente de verdad. Deduplica URLs repetidas y respeta el máximo de resultados configurado.
- **Anti-bot tratado como fallo, no como vacío**: DDG responde 202 con su challenge (`anomaly-modal__check`) cuando detecta un bot; se envían cabeceras de navegador y, si aun así bloquea, se lanza `BusquedaException` → HTTP 503 con mensaje que invita a pegar la URL a mano. "Sin resultados" y "bloqueado" nunca se confunden.
- **Importación selectiva**: `POST /api/vehiculos/{id}/manuales/importar` con `{"url": "..."}`. Descarga, guarda con el mismo `AlmacenamientoArchivos` y dispara el mismo `DocumentoSubidoEvent` (AFTER_COMMIT) que la subida manual: un solo pipeline de procesamiento.
- **SSRF cerrado con tests**: `HttpDescargadorUrl` (solo JDK) acepta únicamente http/https, sigue las redirecciones a mano (`Redirect.NEVER`) para validar CADA salto y rechaza loopback, rangos privados, link-local, CGNAT y metadatos de la nube; si el DNS no se puede verificar, falla en cerrado. Tope de 3 redirecciones y 10 MB durante la lectura.
- **Errores por motivo**: `DescargaException` + `MotivoDescarga` → 400 (URL inválida / host no permitido), 413 (demasiado grande), 415 (tipo no soportado), 502 (fallo de red). Nada se disfraza de 500 genérico.
- **Trazabilidad y deduplicación**: nueva columna `documentos.origen_url` (null en subidas manuales) mostrada como enlace en la UI; reimportar la misma URL devuelve el documento existente (`yaExistia: true`, HTTP 200) sin volver a descargar ni reprocesar.
- **UI**: en la pestaña Documentos, consulta opcional + listado de candidatos con fuente visible y botón "Importar", más un campo para pegar una URL a mano. El texto avisa de que la URL propuesta puede no ser el manual exacto del vehículo.
- **Tests**: la suite pasó de 68 a **158 tests** (141 unitarios + 17 de integración) en `mvn verify`, todos verdes. 84 tests nuevos, incluidos los 12 casos de host bloqueado del validador SSRF y el caso de "fallo a mitad" de la descarga.
- **ADR 005**: documenta la alternativa gratuita elegida, la zona gris de hacer scraping de un tercero y el plan de migración a una API con contrato si hay presupuesto.
- **Documentación corregida**: README, ROADMAP y METRICS ya no mencionan Tavily como integración prevista (era una contradicción con el diseño aprobado).

## 2026-09-04 (Fase 2: procesamiento y embeddings)

- **Extracción de texto**: `TextExtractor` con implementaciones PDFBox (PDF), POI (DOCX) y plain UTF-8 (TXT), selección por `TextExtractorFactory`.
- **Chunking**: `SlidingWindowChunker` (512 chars con overlap 64, ventana deslizante determinista).
- **Embeddings**: `EmbeddingService` + `OpenRouterEmbeddingService` (modelo por defecto `text-embedding-3-small`). La API key se resuelve en el constructor con una única fuente: property `mecania.embedding.api-key` o fallback env var `OPENROUTER_API_KEY`.
- **Procesamiento async**: `DocumentoProcessor` (pool `mecania-proc-` 2/4/100) orquesta extracción→chunking→embeddings→persistencia de fragmentos.
- **Disparo tras commit**: `DocumentoService.subirDocumento` publica `DocumentoSubidoEvent`; el listener con `@TransactionalEventListener(AFTER_COMMIT)` dispara el procesamiento solo después del commit de la transacción.
- **Atomicidad**: `DocumentoProcesadorTransaccional` (bean separado) con `@Transactional(rollbackFor = Exception.class)` — un fallo a mitad revierte todos los fragmentos.
- **Seguridad**: `AlmacenamientoDiscoLocal` sanitiza nombres de archivo y rutas contra path traversal (`..`); lectura/borrado rechazan rutas que escapen del baseDir.
- **Tests**: la suite pasó de 26 a **68 tests** (62 unitarios + 6 integración) en `mvn verify`.
- **ADR 004**: documenta la estrategia de embeddings y procesamiento asíncrono.

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
