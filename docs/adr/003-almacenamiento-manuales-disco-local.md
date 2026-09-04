# ADR 003: Estrategia de almacenamiento de manuales subidos

**Estado:** Aceptado   
**Fecha:** 2026-09-04   

## Contexto
Mecania necesita permitir a los usuarios subir manuales de sus vehículos (PDF/TXT/DOCX) y asociarlos a un vehículo concreto. Estos archivos deben persistir entre reinicios del servidor y ser descargables. En fases posteriores (Fase 2), se procesarán para generar embeddings y alimentar el chat RAG.

Hay tres alternativas razonables para almacenar los archivos físicos:
1. Disco local del servidor (ruta configurable).
2. Columna binaria (`BYTEA`/`BYTEA`/OID) en PostgreSQL.
3. Servicio externo de object storage (Supabase Storage, S3, Cloudflare R2, MinIO).

## Decisión
Implementamos **almacenamiento en disco local** mediante una interfaz `AlmacenamientoArchivos` con implementación `AlmacenamientoDiscoLocal`, configurable vía `application.properties` (`mecania.storage.dir`, por defecto `${user.home}/mecania-storage`). En la BD solo se guarda la ruta relativa al directorio base, no el archivo.

## Alternativas consideradas

### Alternativa 1: Disco local (elegida)
- **Pros**:
  - Cero coste externo.
  - Cero credenciales que gestionar.
  - Fácil de inspeccionar y depurar (`ls` o explorador de archivos).
  - Funciona perfectamente en local y en un VPS sin dependencias adicionales.
  - Permite volcar los archivos a un volumen Docker persistente con un cambio mínimo de configuración.
- **Contras**:
  - No escala horizontalmente sin NFS/GlusterFS/compartir volumen.
  - Backup debe incluir el directorio, no solo la BD.
  - Si se reinicia el contenedor sin volumen persistente, se pierden los archivos.

### Alternativa 2: Columna binaria en PostgreSQL
- **Pros**:
  - Backup unificado (BD + archivos en un mismo dump).
  - No requiere sistema de archivos adicional.
- **Contras**:
  - Inflar la BD con archivos grandes degrada el rendimiento de las consultas.
  - Difícil servir el archivo directamente sin pasar por la aplicación.
  - Limita el tamaño del archivo al `max_wal_size` o configuración de PostgreSQL.
  - En la práctica, los archivos de manuales (>5MB) son demasiado grandes para vivir en una columna binaria sin penalizar otras operaciones.

### Alternativa 3: Servicio externo (Supabase Storage, S3, etc.)
- **Pros**:
  - Escala horizontalmente sin más configuración.
  - URLs firmadas permiten descarga directa sin pasar por el backend.
  - Servicio gestionado: alta disponibilidad y backups incluidos.
- **Contras**:
  - **Coste**: Supabase Pro empieza en ~$25/mes; S3/R2 también tienen coste (aunque bajo para volúmenes pequeños).
  - **Dependencia externa y credenciales**: añade configuración (URL, keys, buckets) que debe mantenerse sincronizada entre entornos.
  - **Complejidad de tests**: los tests de integración necesitarían mocks o LocalStack/Minio local, lo que aumenta el tiempo de setup del CI.
  - **Verificación de pgvector en tier gratuito**: a confirmar; actualmente no entra en free, lo que obligaría a plan Pro o workaround.

## Consecuencias

### Positivas
- Implementación inmediata y verificable en local sin más setup.
- Cero coste externo mientras el proyecto es portfolio.
- La abstracción `AlmacenamientoArchivos` permite sustituir la implementación en una migración futura (Supabase Storage, S3) tocando solo una clase.
- El directorio de almacenamiento puede mapearse a un volumen Docker persistente en `docker-compose.yml` o `render.yaml` con un cambio trivial.

### Negativas
- Backup de producción debe incluir el directorio de manuales, no solo la BD.
- No apto para múltiples instancias del backend apuntando al mismo almacenamiento sin volumen compartido.
- Si el filesystem local se llena, no hay failover automático.

## Plan de migración a Supabase Storage (cuando se decida)
1. Crear proyecto Supabase y bucket `manuales` (privado).
2. Crear `AlmacenamientoSupabase` que implemente `AlmacenamientoArchivos` usando el SDK de Supabase Storage.
3. Marcar la implementación con `@ConditionalOnProperty(name = "mecania.storage.provider", havingValue = "supabase")`.
4. Migrar los archivos existentes con un script de una sola ejecución.
5. Actualizar el ADR para marcar Supabase como implementación activa.

Este plan está documentado para que cuando se decida desplegar en producción real, la decisión ya tenga un camino marcado.

## Nota de revisión
Se revisará cuando se decida pasar a entorno de producción real o cuando el volumen de manuales por usuario haga inviable el almacenamiento local.