# ADR 009: Plataformas de despliegue (dónde vive la app, dónde la base, cómo se llega desde internet)

**Estado:** Implementado y en producción
**Fecha:** 2026-09-17
**ADRs relacionados:** [ADR-001](001-stack-tecnologico.md), [ADR-010](010-almacenamiento-objetos-s3.md)

## Contexto

Mecania se terminó como proyecto de portafolio y hay que ponerlo accesible desde internet con dominio propio y HTTPS real, con una base de datos gestionada (no un Postgres en contenedor). Restricciones: coste cero o casi (es una pieza que no genera ingresos), operación mínima (una sola persona detrás) y que la arquitectura siga el estándar de los proyectos de KAVANA (base gestionada, no Postgres montado a mano).

## Decisión

- **La aplicación corre en Render, como Web Service Docker, región Frankfurt, plan gratuito.** La imagen se construye desde el mismo Dockerfile del repo; Render hace el health check sobre `/health`; cada despliegue se lanza por la API (`/v1/services/{id}/deploys`) contra el commit bueno.
- **La base de datos es Neon, proyecto `mecania`, región `eu-central-1`, PostgreSQL 16 con pgvector**, plan gratuito (0,5 GB, 100 CU-h/mes, escala a cero a los 5 min, instant restore de 6 h y 1 snapshot). Se usa el host directo (no el `-pooler` de PgBouncer), porque Hibernate rompe con el pooling en modo transacción.
- **El acceso desde internet es `https://mecania.kavanasystems.com`**: subdominio de un dominio ya contratado en Namecheap, apuntado al dominio por defecto de Render vía CNAME (`mecania` → `mecania-97wf.onrender.com`). El HTTPS lo emite Render (certificado Google Trust Services, renovado automáticamente; verificado con `curl` y `openssl`). El apex y `www` de `kavanasystems.com` siguen en Vercel, sin colisión con este registro.
- **Los manuales viven en Cloudflare R2** (ADR-010), porque el disco del contenedor es efímero.

## Alternativas evaluadas y descartadas

| Alternativa | Motivo del descarte |
|---|---|
| VPS propio (el de preproducción) | Ya lo usa preproducción y Hermes; exponer producción sobre él mezcla planos y añade mantenimiento de seguridad del S.O. |
| k3s / cluster en el VPS | Sobreingeniería absoluta para un servicio; el VPS no necesita orquestar nada. |
| Fly.io | Descartado por la casa (sin cuenta activa); además añade un segundo proveedor de contenedores para algo que Render ya da. |
| Vercel / Railway para la API Spring Boot | Vercel es para estáticos/edges, no un runtime Java con BD; Railway es más caro por lo que da. El estático `index.html` bien servido por la propia app (Render) evita separar front y back del portafolio. |
| Postgres gestionado de Render | A partir de ~6 $/mes y con free tier que se autodestruye a los 30 días; Neon da más (snapshots, instant restore, escala a cero) gratis. |
| Supabase (para la BD) | Su Postgres vía pooler/IPv6 dio problemas y duplica proveedor; para el contenedor PostgreSQL+pgvector señalado por el estándar de la casa, Neon es más directo. |
| AWS Aurora/RDS | Coste y gestión IAM desproporcionados para una pieza de portafolio. |

## Consecuencias

**Positivas**
- Coste de infraestructura **0 $/mes** dentro de los niveles gratuitos de los tres proveedores (verificado en el informe de Fase 3: coste, operación y rollback).
- Base gestionada de verdad, con pgvector listo y ventana de restauración de 6 horas sin pagar.
- Despliegue reproducible por API contra un commit concreto (lo que hace el rollback de código trivial: redeploy del SHA bueno).

**Negativas y riesgos asumidos (bien conocidos)**
- **Render free duerme el servicio a los 15 min sin tráfico** y el arranque en frío tarda 30-90 s. Asumido por ser portafolio; el salto a Starter (7 $/mes) es la única decisión que cambia esto.
- **Neon free suspende el compute** al agotar CU-h/egress y bloquea escrituras al superar 0,5 GB. Umbrales medidos y lejos del uso real.
- La preproducción de desarrollo (VPS) sigue en PostgreSQL 15, mientras producción es 16: dos versiones conviviendo. Documentado (docker-compose fijado a PG 15) y pendiente de migrar el contenedor de desarrollo cuando se quiera uniformar.
- Sin migraciones versionadas todavía (Hibernate `ddl-auto=update`) y sin backup automático de la BD: pendientes con ticket (B3).

## Verificación (2026-09-17)

- `https://mecania.kavanasystems.com/` → 200; `/health` → 200 `UP` (SELECT 1 real contra Neon); `/api/vehiculos` sin token → 401; `/health/ready` → 200 con BD, embeddings y almacenamiento R2 en `UP`.
- Certificado Google Trust Services, `CN = mecania.kavanasystems.com`, válido (renovación automática de Render).
- E2E de punta a punta en producción: registro → vehículo → subida de manual → `LISTO` con vectores → chat RAG con fuentes; y persistencia tras redespliegue (hash idéntico) con el almacenamiento en R2.