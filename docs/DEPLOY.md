# Despliegue de Mecania

Mecania es un backend Java 21 + Spring Boot con PostgreSQL + pgvector. El
despliegue de portfolio sigue el estándar KAVANA: servicio web en **Render**
(Docker) y base de datos en **Neon** (PostgreSQL con pgvector). El VPS es
laboratorio de desarrollo, no producción.

## Arquitectura

| Pieza | Servicio | Notas |
|-------|----------|-------|
| Backend (Spring Boot) | Render (Docker) | `Dockerfile` multi-etapa, `java -jar` |
| Base de datos (Postgres + pgvector) | Neon | extensión `vector` habilitada |
| CI | GitHub Actions | `mvn verify` en cada push a `main` |

No hay frontend separado: la UI (`index.html`) la sirve el propio backend.

## Requisitos previos

- Repo subido a GitHub (`kavanasystemsinfo-ui/mecania`).
- Cuenta de Render y de Neon (del titular del proyecto).
- `OPENROUTER_API_KEY` (para embeddings y chat) y un `JWT_SECRET` propio.

## 1. Base de datos en Neon

1. Crear un proyecto en Neon (región cercana a España, ej. Frankfurt).
2. Copiar la cadena de conexión (`postgresql://usuario:contraseña@host/db`).
3. Neon trae pgvector: el propio arranque de la app ejecuta
   `CREATE EXTENSION IF NOT EXISTS vector` (ver `PgVectorSchemaInitializer`),
   así que no hay que crear la extensión a mano.

## 2. Servicio web en Render

1. Render → New → Web Service → conectar el repo de GitHub.
2. Configurar el servicio (formulario):

| Campo | Valor |
|-------|-------|
| Runtime | Docker |
| Health Check Path | `/health` |
| Env vars | ver tabla de abajo |

3. Variables de entorno (todas marcadas como secret):

| Variable | Valor |
|----------|-------|
| `DATABASE_URL` | la cadena de Neon (`postgresql://...`) |
| `DB_USERNAME` | usuario de Neon |
| `DB_PASSWORD` | contraseña de Neon |
| `JWT_SECRET` | cadena aleatoria de al menos 32 caracteres |
| `OPENROUTER_API_KEY` | tu clave de OpenRouter |

Render hace ping a `/health` (200 = la app y la BD están vivas). No requiere
autenticación: está fuera de `/api/**`.

También hay un `render.yaml` (Blueprint) con la misma configuración por si se
prefiere desplegar con `render blueprint`.

## 3. Verificar el despliegue

```bash
# health check
curl -s https://<servicio>.onrender.com/health          # {"status":"UP"}

# registro → login → crear vehículo
curl -s -X POST https://<servicio>.onrender.com/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@test.com","password":"secreto123"}'
```

## 4. Secretos y seguridad

- `JWT_SECRET` y `OPENROUTER_API_KEY` viven SOLO en las env vars del servicio,
  nunca en el repo. En desarrollo se usan los defaults de
  `application.properties`.
- La BD de desarrollo (docker-compose, puerto 5433) usa `mecania`/`mecania`; la
  de producción usa las credenciales de Neon.

## Notas de producción

- `spring.jpa.hibernate.ddl-auto=update` auto-crea el esquema (útil para el
  MVP); en producción real se migraría a Flyway/Liquibase.
- `spring.jpa.show-sql=true` y el SQL logging en DEBUG son para desarrollo; en
  producción conviene ponerlos a `false`/`INFO`.
- El log va a consola y a `logs/mecania.log` con rotación (10 MB, 7 días,
  tope 100 MB) vía `logback-spring.xml`. En Render el sistema de archivos es
  efímero: los logs de archivo sirven en local, en Render se lee la consola.
