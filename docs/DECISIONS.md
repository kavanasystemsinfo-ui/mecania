# Índice de Decisiones de Arquitectura (ADRs)

Este archivo lista todas las decisiones arquitectónicas documentadas en forma de ADR (Architecture Decision Record). Cada ADR resume el contexto, la decisión, las alternativas evaluadas y las consecuencias.

## Índice de ADRs

| # | Título | Estado | Fecha |
|---|--------|--------|-------|
| [ADR-001](docs/adr/001-stack-tecnologico.md) | Stack: Java 21 + Spring Boot 3.4 + PostgreSQL + pgvector | ✅ Implementado | 2026-09-03 |
| [ADR-002](docs/adr/002-seguridad-abierta-mvp.md) | Seguridad abierta en fase MVP (permitAll) | ✅ Implementado | 2026-09-04 |

## Resumen ejecutivo (una línea por ADR)

1. **ADR-001 — Stack tecnológico.** Elegimos Java 21 + Spring Boot 3.4 como backend, PostgreSQL 16 con extensión pgvector para persistencia y búsqueda vectorial, y Springdoc OpenAPI para documentación API. Este stack coincide con ofertas enterprise Java en España y permite demostrar habilidades relevantes para el mercado laboral.
2. **ADR-002 — Seguridad en MVP.** Para acelerar la validación de features core durante el desarrollo inicial, mantenemos seguridad abierta (`permitAll()`) y desactivamos CSRF. Esta decisión es consciente y temporal; se documenta explícitamente para evitar suposiciones de producción. La autenticación real (probablemente JWT) se introducirá en una fase posterior cuando sea necesario para demostrar manejo de identidad de usuario.

## Verificación de la documentación contra el código

- Los ADRs reflejan el estado actual del código. Cada decisión incluye alternativas consideradas y consecuencias, permitiendo a un nuevo ingeniero entender el trade-off tomado.
- El archivo `docs/adr/001-stack-tecnologico.md` y `docs/adr/002-seguridad-abierta-mvp.md` están presentes y verificados.
