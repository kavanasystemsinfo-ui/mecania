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

`/health/ready` es más estricto y es el que hay que mirar al verificar un
despliegue: comprueba además que la clave de embeddings está configurada y que
el directorio de almacenamiento acepta escrituras, y devuelve 503 si alguna de
las tres falla. No se usa como health check de la plataforma a propósito: un
proveedor de IA caído no debe provocar reinicios en bucle de la aplicación.

También hay un `render.yaml` (Blueprint) con la misma configuración por si se
prefiere desplegar con `render blueprint`.

## 3. Verificar el despliegue

```bash
# health check rápido (el que usa Render)
curl -s https://<servicio>.onrender.com/health          # {"status":"UP"}

# readiness honesto: BD + clave de embeddings + almacenamiento
curl -s https://<servicio>.onrender.com/health/ready    # {"status":"UP","checks":{...}}

# registro → login → crear vehículo
curl -s -X POST https://<servicio>.onrender.com/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@test.com","password":"secreto123"}'
```

## 4. Secretos y seguridad

- `JWT_SECRET` y `OPENROUTER_API_KEY` viven SOLO en las env vars del servicio,
  nunca en el repo. `mecania.jwt.secret` **no tiene valor por defecto**: si
  `JWT_SECRET` falta al arrancar, la aplicación falla en el arranque en vez de
  firmar tokens con un secreto publicado en el repositorio. Un despliegue mal
  configurado debe romper, no quedarse abierto. Los perfiles de test definen su
  propio secreto en `src/test/resources/application-test*.properties`.
- La BD de desarrollo (docker-compose, puerto 5433) usa `mecania`/`mecania`; la
  de producción usa las credenciales de Neon.

## Notas de producción

- **Límite de subida: 25 MB**, y se configura en dos sitios que tienen que ir
  juntos: `spring.servlet.multipart.max-file-size` / `max-request-size` (corte
  del servidor de aplicaciones) y `mecania.upload.max-bytes` (validación del
  servicio). Si el corte del servidor es menor, el usuario recibe un 413 aunque
  el fichero esté por debajo del límite anunciado. Pasarse devuelve **413** con
  `archivo_demasiado_grande` y el límite en el mensaje, nunca un 500. La
  importación desde internet mantiene su propio tope de 10 MB
  (`mecania.descarga.max-bytes`), más bajo a propósito porque esa vía descarga
  de terceros.
- `spring.jpa.hibernate.ddl-auto=update` auto-crea el esquema (útil para el
  MVP); en producción real se migraría a Flyway/Liquibase.
- `spring.jpa.show-sql=true` y el SQL logging en DEBUG son para desarrollo; en
  producción conviene ponerlos a `false`/`INFO`.
- El log va a consola y a `logs/mecania.log` con rotación (10 MB, 7 días,
  tope 100 MB) vía `logback-spring.xml`. En Render el sistema de archivos es
  efímero: los logs de archivo sirven en local, en Render se lee la consola.
