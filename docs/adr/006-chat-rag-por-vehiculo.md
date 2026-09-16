# ADR 006: Chat RAG por vehículo con pgvector

- **Estado:** Implementado
- **Fecha:** 2026-09-16

## Contexto

La Fase 4 pide un chat que responda usando SOLO los manuales del vehículo del
usuario. Eso exige dos piezas que faltaban: persistir el embedding de cada
fragmento (hasta ahora se calculaba y se descartaba) y recuperarlos por
similitud semántica restringida al vehículo. Hibernate-core no sabe mapear el
tipo `vector` de pgvector, así que la persistencia no puede ir por el modelo JPA.

## Decisión

1. **Vectores fuera del modelo JPA.** Los embeddings se guardan en una tabla
   auxiliar `fragmento_embeddings (fragmento_id, embedding vector)` gestionada
   por JDBC nativo (`JdbcTemplate` + `PGobject` tipo "vector"). Hibernate no la
   toca. La crea un `ApplicationRunner` idempotente
   (`PgVectorSchemaInitializer`) que ejecuta `CREATE EXTENSION IF NOT EXISTS
   vector` y `CREATE TABLE IF NOT EXISTS fragmento_embeddings`, solo sobre
   PostgreSQL (sobre H2 no hace nada).
2. **Puerto `RepositorioVectores` en dominio.** `guardar(fragmentoId, embedding)`
   y `buscarSimilares(vehiculoId, consulta, topK)`. La implementación
   `PgVectorRepositorioVectores` usa el operador `<=>` (distancia coseno) y
   **filtra SIEMPRE por `d.vehiculo_id = ?`**: no existe camino para cruzar
   manuales de vehículos distintos.
3. **Persistencia atómica.** El procesador guarda texto (JPA) y vector (JDBC)
   en la misma transacción; un fallo del almacén de vectores revierte también
   los fragmentos (el `JdbcTemplate` participa de la transacción JPA por el
   comportamiento estándar de Spring Boot con un único DataSource).
4. **Chat con umbral de similitud.** `ChatManualesService` vectoriza la
   pregunta, recupera top-K por coseno y descarta los que queden por debajo de
   `mecania.chat.similitud-minima` (0,2). Si no queda ninguno, responde "sin
   base" **sin llamar al LLM**: el modelo nunca recibe una pregunta sin
   contexto y no puede inventar.
5. **LLM con fuentes verificables.** La respuesta la genera `OpenRouterLlmService`
   (chat/completions, modelo `openai/gpt-4o-mini` por defecto) con un prompt de
   sistema que obliga a responder solo con los fragmentos. La API devuelve
   `RespuestaChat` con la respuesta y las fuentes (fragmentos) que la sostienen.

## Alternativas consideradas

- **Columna `vector` en `fragmentos` con custom Hibernate type / hibernate-types**:
  añade dependencia y complejidad; con `ddl-auto=update` la columna se perdería
  al recrear la tabla.
- **Flyway para migraciones**: correcto en producción, pero el repo hoy usa
  `ddl-auto=update`; introducir Flyway era un cambio mayor sin valor para una
  pieza de portfolio.
- **Vector DB externa (Pinecone, Weaviate)**: ya descartado en ADR 001/ROADMAP;
  pgvector cubre el caso sin dependencia nueva ni salida de datos.
- **Sin umbral de similitud**: devolvería contexto irrelevante y el LLM podría
  alucinar. Se elige umbral configurable, por defecto 0,2.

## Consecuencias

- El esquema pgvector lo gestiona el arranque de la app (idempotente), no una
  herramienta de migraciones. El tipo `vector` se crea sin dimensión fija, así
  que un cambio de modelo de embeddings no rompe la columna; solo habría que
  reindexar los datos antiguos.
- El coste del chat depende del modelo (gpt-4o-mini es barato; hay alternativas
  `:free` en OpenRouter). Configurable por `mecania.chat.model`.
- La query SQL de pgvector no se cubre en la suite (H2 no tiene pgvector): se
  verifica con el repositorio mockeado en tests y con un smoke test real contra
  PostgreSQL.
