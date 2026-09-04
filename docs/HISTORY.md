# HISTORY.md

Evolución del proyecto Mecania y decisiones descartadas.

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
