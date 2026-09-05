# Índice de Decisiones de Arquitectura (ADRs)

Este archivo lista todas las decisiones arquitectónicas documentadas en forma de ADR (Architecture Decision Record). Cada ADR resume el contexto, la decisión, las alternativas evaluadas y las consecuencias.

## Índice de ADRs

| # | Título | Estado | Fecha |
|---|--------|--------|-------|
| [ADR-001](docs/adr/001-stack-tecnologico.md) | Stack: Java 21 + Spring Boot 3.4 + PostgreSQL + pgvector | ✅ Implementado | 2026-09-03 |
| [ADR-002](docs/adr/002-seguridad-abierta-mvp.md) | Seguridad abierta en fase MVP (permitAll) | ✅ Implementado | 2026-09-04 |
| [ADR-003](docs/adr/003-almacenamiento-manuales-disco-local.md) | Almacenamiento de manuales: disco local con abstracción preparada para Supabase Storage | ✅ Implementado | 2026-09-04 |
| [ADR-004](docs/adr/004-embeddings-y-procesamiento-async.md) | Estrategia de embeddings y procesamiento asíncrono (extracción, chunking, OpenRouter, @Async con AFTER_COMMIT) | ✅ Implementado | 2026-09-04 |

## Resumen ejecutivo (una línea por ADR)

1. **ADR-001 — Stack tecnológico.** Elegimos Java 21 + Spring Boot 3.4 como backend, PostgreSQL 16 con extensión pgvector para persistencia y búsqueda vectorial, y Springdoc OpenAPI para documentación API. Este stack coincide con ofertas enterprise Java en España y permite demostrar habilidades relevantes para el mercado laboral.
2. **ADR-002 — Seguridad en MVP.** Para acelerar la validación de features core durante el desarrollo inicial, mantenemos seguridad abierta (`permitAll()`) y desactivamos CSRF. Esta decisión es consciente y temporal; se documenta explícitamente para evitar suposiciones de producción. La autenticación real (probablemente JWT) se introducirá en una fase posterior cuando sea necesario para demostrar manejo de identidad de usuario.
3. **ADR-003 — Almacenamiento.** Guardamos los manuales en disco local (`mecania.storage.dir`, default `${user.home}/mecania-storage`) mediante una interfaz `AlmacenamientoArchivos`. La elección es temporal: cuando se decida desplegar a producción real, se sustituye por `AlmacenamientoSupabase` (o S3) implementando la misma interfaz, sin tocar el resto del código.
4. **ADR-004 — Embeddings y procesamiento async.** Extracción (PDFBox/POI/plain), chunking (ventana deslizante 512/64), embeddings vía OpenRouter (`text-embedding-3-small`) y procesamiento `@Async` en pool dedicado. El disparo se hace por evento `@TransactionalEventListener(AFTER_COMMIT)` para evitar la carrera async/commit; el paso transaccional usa `rollbackFor = Exception.class` para atomicidad real (los embeddings no se persisten hasta fase 4, por limitación de Hibernate con el tipo `vector` de pgvector).

## Verificación de la documentación contra el código

- Los ADRs reflejan el estado actual del código. Cada decisión incluye alternativas evaluadas y consecuencias, permitiendo a un nuevo ingeniero entender el trade-off tomado.
- Los archivos `docs/adr/001-stack-tecnologico.md`, `docs/adr/002-seguridad-abierta-mvp.md`, `docs/adr/003-almacenamiento-manuales-disco-local.md` y `docs/adr/004-embeddings-y-procesamiento-async.md` están presentes y verificados.
- **Tests reales**: 68 tests (62 unitarios + 6 de integración), todos verdes en `mvn verify` (Java 21). Detalle por suite en `docs/METRICS.md`. Cifra verificada ejecutando la suite, no por grep.
- **Verificación end-to-end**: tras implementar la subida de documentos, se ha ejecutado un flujo completo vía `curl` (crear vehículo → subir PDF/TXT → listar → descargar) y se ha confirmado que el contenido descargado coincide con el original.