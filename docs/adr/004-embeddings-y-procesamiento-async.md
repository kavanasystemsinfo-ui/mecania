# ADR 004: Estrategia de embeddings y procesamiento asíncrono

**Estado:** Aceptado   
**Fecha:** 2026-09-04   

## Contexto
Tras implementar la subida de manuales (Fase 1), el siguiente paso es hacer que esos manuales sean consultables por el chat RAG (Fase 4). Para ello necesitamos:
1. Extraer el texto plano del archivo subido (PDF, TXT, DOCX).
2. Trocearlo en fragmentos (chunks) de tamaño manejable.
3. Calcular un vector de embedding por fragmento.
4. Persistir los vectores para poder buscar por similitud en preguntas futuras.

Además, hay un problema operativo: el cálculo de embeddings es lento y, si se usa una API externa, cuesta dinero. Si lo hacemos dentro del endpoint HTTP que sube el archivo, el usuario espera minutos y un fallo deja el documento en estado inconsistente.

## Decisión

### Extracción de texto
- **TXT**: lectura directa UTF-8 (sin librería extra).
- **PDF**: **Apache PDFBox 3.0.x** (licencia Apache 2.0, estable, sin dependencias nativas).
- **DOCX**: **Apache POI 5.x** (licencia Apache 2.0, mismo ecosistema que PDFBox).
- Patrón: interfaz `TextExtractor` con implementación por tipo (`PdfBoxTextExtractor`, `PoiTextExtractor`, `PlainTextExtractor`). Selección por tipo en un factory simple.

### Chunking
- **Tamaño**: 512 caracteres por chunk.
- **Overlap**: 64 caracteres (12.5 %).
- **Justificación**: 512 caracteres es el default de LangChain y un buen balance entre:
 - Suficiente contexto para que un fragmento sea semánticamente útil (~80-100 palabras, una o dos frases completas).
 - Suficientemente pequeño para que el embedding capture una idea concreta en lugar de mezclar varias.
 - El overlap del 12.5 % evita que una frase quede cortada entre dos chunks sin contexto.
- Implementación: ventana deslizante por caracteres (no por tokens; para embeddings simples el corte por caracteres es suficiente y predecible).
- **Nota**: en una iteración futura se podría migrar a chunking por tokens (con `jtokkit` o similar) si se observa que los cortes por caracteres rompen palabras técnicas. Hoy no es necesario.

### Embeddings
- **Proveedor por defecto**: OpenRouter (mismo proveedor que la fase de chat RAG, evita fragmentar el ecosistema de credenciales).
- **Modelo por defecto**: `text-embedding-3-small` (1536 dimensiones, ~$0.02 por millón de tokens, latencia media).
- **Dimensión**: configurable vía `mecania.embedding.dimension` (default 1536). El modelo concreto se elige en `mecania.embedding.model`.
- **Implementación**: interfaz `EmbeddingService` con `OpenRouterEmbeddingService`. La llamada HTTP se hace con `RestTemplate` (bean `embeddingRestTemplate` en `RestClientConfig`, timeouts 10s/30s). En tests se usa un mock.
- **Si la API falla**: el documento se marca como `ERROR` con el mensaje de la excepción. No se reintenta automáticamente en esta fase.

### Persistencia del vector
- **Tipo SQL**: `vector(N)` de pgvector, donde N = `mecania.embedding.dimension`.
- **Migración**: SQL manual en `db/migration/V1__embedding_vector.sql` ejecutado por el operador (o por un script Flyway/Liquibase si se introduce en el futuro).
- **Mapping JPA**: hibernate-core no soporta `vector` directamente. Se opta por una de estas dos opciones (a decidir en implementación):
 - **Opción 2a**: columna `embedding` de tipo `vector` mapeada como `String` (representación textual del vector, p.ej. `"[0.1,0.2,...]"`). Hibernate no la lee pero la deja pasar. El acceso al vector se hace con `@Query(nativeQuery=true)` cuando se necesite similarity search en fase 4.
 - **Opción 2b**: usar `hibernate-types-60` para mapear `vector` a `float[]`. Añade una dependencia más.
- Decisión final: **opción 2a** (más simple, suficiente para fase 2; similarity search usará queries nativas igualmente en fase 4).
- **Los embeddings por fragmento** se guardan en una columna `embedding` separada en la tabla `fragmentos`. La columna `embedding` actual en `documentos` queda obsoleta y se eliminará (ver nota de revisión).

### Procesamiento asíncrono
- **Spring `@Async`** sobre `DocumentoProcessor.procesar(Long vehiculoId, Long documentoId)`.
- **`@EnableAsync`** en una clase de configuración `AsyncConfig`.
- **Pool dedicado**: `ThreadPoolTaskExecutor` con `corePoolSize=2`, `maxPoolSize=4`, `queueCapacity=100`, `threadNamePrefix="mecania-proc-"`. Configurable vía properties.
- **Disparo**: el `DocumentoService.subirDocumento` deja el documento en estado `PROCESANDO` y publica un evento `DocumentoSubidoEvent`. Un listener `@TransactionalEventListener(phase = AFTER_COMMIT)` dispara el procesamiento async SOLO cuando la transacción ya ha commiteado. Lanzar un método @Async dentro del método transaccional no garantiza que el hilo vea las entidades commiteadas (carrera real: quedaba en PROCESANDO para siempre); el evento AFTER_COMMIT garantiza el orden.
- **Manejo de errores**: si el procesamiento lanza una excepción, el documento se marca como `ERROR` con el mensaje. El usuario lo verá en el listado.
- **Atomicidad**: el paso transaccional (`DocumentoProcesadorTransaccional`) usa `@Transactional(rollbackFor = Exception.class)` — OBLIGATORIO porque `EmbeddingException`/`ExtractionException` son checked y el default de Spring solo revierte unchecked. Sin eso, un fallo a mitad commitea los fragmentos ya guardados (detectado por test de integración con fallo en el 2º fragmento). Separar el paso transaccional en un bean propio también evita la self-invocation de `@Transactional` (llamada interna no pasa por el proxy).

## Alternativas consideradas

### Chunking
- **Por tokens (con `jtokkit`)**: más preciso pero añade complejidad. Descartado para esta fase; se documenta como evolución natural.
- **Por párrafos (respetando saltos de línea)**: más legible pero impredecible en PDFs mal formateados. Se descartó por inconsistencia.
- **Sin overlap**: más simple pero pierde contexto en los bordes. Descartado por degradar la calidad del RAG.

### Embeddings
- **Modelo local (sentence-transformers via DJL/PyTorch)**: gratis y sin dependencia externa. Descartado por complejidad de setup (JVM nativa, GPU opcional) y por no aportar mucho valor en un portfolio donde lo importante es la integración, no el rendimiento.
- **OpenAI directo**: similar a OpenRouter pero añade otro proveedor de credenciales. Se eligió OpenRouter por unificación.
- **Cohere / Voyage / otros**: buenas opciones pero sin ventaja clara para un MVP de portfolio.

### Procesamiento
- **Síncrono dentro del endpoint**: descartado por mala UX (esperas largas) y porque un fallo deja el documento inconsistente.
- **Cron periódico**: añade infraestructura (scheduler). Descartado por sobredimensionado para la fase actual.
- **Cola de mensajes (RabbitMQ, Redis Streams)**: overkill para un portfolio. Documentado como evolución natural cuando se quiera escalar.

## Consecuencias

### Positivas
- El usuario sube un manual y la UI vuelve inmediata; el procesamiento ocurre en background.
- Los embeddings se almacenan en pgvector, listos para similarity search en la fase 4.
- La abstracción `EmbeddingService` permite cambiar de proveedor (o a local) sin tocar el resto.
- Tests pueden usar mocks del servicio de embeddings, sin depender de la API real.

### Negativas
- Hay que introducir `@EnableAsync` y un pool de hilos: nuevo punto de configuración a mantener.
- Si la API de OpenRouter está caída, los documentos quedan en `ERROR` hasta que se relance manualmente (no hay reintento).
- La columna `embedding` en `documentos` queda obsoleta: hay que eliminarla en una migración posterior.

## Plan de migración cuando se decida desplegar
1. Introducir Flyway o Liquibase (en lugar de SQL suelto) si el proyecto va a producción real.
2. Añadir reintentos con backoff exponencial para llamadas a OpenRouter.
3. Monitorizar el pool de procesamiento (tareas en cola, tareas fallidas).
4. Considerar mover el procesamiento a un worker externo (microservicio) si el volumen lo justifica.

## Nota de revisión
Se revisará cuando se implemente la fase 4 (chat RAG) y se necesite probar la calidad de las respuestas. Si los cortes por caracteres degradan la calidad, se migrará a chunking por tokens.