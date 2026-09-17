# Mecania

![Java](https://img.shields.io/badge/Java-21-red)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![pgvector](https://img.shields.io/badge/pgvector-enabled-orange)
![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0-lightgrey)
![Tests](https://img.shields.io/badge/tests-239-brightgreen)
![License](https://img.shields.io/badge/license-MIT-yellow)

## 🎯 Qué es Mecania y por qué existe (como pieza de portafolio)

**Mecania** es un **MVP DEMO/PORTFOLIO** construido en Java/Spring Boot para demostrar arquitectura backend, análisis de negocio y decisiones de diseño en el dominio de mantenimiento vehicular. **NO es un producto para cliente real**, sino un ejemplo de cómo abordar el desarrollo de software mostrando arquitectura técnica y decisiones de diseño documentadas.

Este proyecto muestra:
- Arquitectura hexagonal limpia con dominio rico (`Vehiculo`, `Documento`, `Fragmento`, `Alerta`).
- Persistencia con PostgreSQL y extensión pgvector para la búsqueda semántica del chat RAG.
- API REST completa, testeada y documentada con OpenAPI/Swagger.
- Decisiones arquitectónicas documentadas en forma de ADRs.
- Documentar decisiones y limitaciones, incluidas las que se derivan del presupuesto (ver la sección «Cómo está construido y cómo lo construiría con presupuesto» y el ADR-011).

## 🏗️ Problema que Mecania intenta resolver

Los propietarios de vehículos tienen dificultades para acceder rápidamente a información técnica específica de su coche: manuales de usuario, guías de mantenimiento, diagramas de piezas, avisos de recall o soluciones a averías comunes. La información suele estar dispersa en PDFs oficiales, foros especializados o webs de fabricantes, y no está organizada por vehículo ni fácilmente buscable en lenguaje natural.

**Problema hipotético:** Falta de un asistente especializado que, dado un vehículo concreto (marca, modelo, año, kilometraje), pueda responder preguntas técnicas usando únicamente la documentación correspondiente a ese vehículo, sin mezclar información de otros modelos ni respuestas genéricas de internet.

> Nota importante: Este escenario es de demostración. No tengo experiencia directa en talleres mecánicos, pero sí he observado el dolor al buscar manuales y guías mientras trabajaba como conductor de reparto.

## 🔧 Stack y arquitectura

Mecania está construido como una aplicación Spring Boot monolítica con separación clara de responsabilidades mediante capas (hexagonal simplificada):

```mermaid
graph LR
    A[Frontend Bootstrap] -->|HTTP| B[API Spring Boot]
    B -->|Driver| C[(PostgreSQL + pgvector)]
    B -->|HTTP| D[DuckDuckGo HTML (búsqueda asistida de manuales)]
    B -->|HTTP| E[LLM via OpenRouter (RAG)]
```

### Capas clave

- **Dominio (`domain/`)**: entidades con lógica de negocio (`Vehiculo`, `Documento`, `Fragmento`, `Alerta`), repositorios, servicios y excepciones.
- **Aplicación (`application/`)**: DTOs y casos de uso simples (p.ej. `UsuarioService`).
- **Infraestructura (`infrastructure/`)**: implementaciones de repositorios (JPA).
- **Interfaz web (`controller/`)**: controladores REST que exponen la API.
- **Frontend estático (`static/`)**: interfaz Bootstrap básica para crear/listar/editar/eliminar vehículos (demo manual).

### Tecnologías

- **Backend:** Java 21 + Spring Boot 3.4
- **Persistencia:** PostgreSQL 16 con extensión pgvector (para almacenar embeddings de fragmentos de texto)
- **Búsqueda externa:** DuckDuckGo (endpoint HTML público) parseado con Jsoup, para proponer manuales sin depender de una API de pago
- **LLM:** Integración con modelos de lenguaje vía OpenRouter (para generar respuestas basadas en fragmentos recuperados)
- **Documentación API:** Springdoc OpenAPI (Swagger UI disponible en `/swagger-ui.html`)
- **Build:** Maven 3.9+
- **Tests:** JUnit 5 + Mockito + Testcontainers (PostgreSQL real en pruebas de integración)
- **Contenedores:** Docker + Docker Compose (para PostgreSQL con pgvector)
- **Frontend (post-MVP):** Bootstrap 5.3 (puede evolucionar a Angular/React si se desea mostrar habilidades frontend)

## 💰 Cómo está construido y cómo lo construiría con presupuesto

Mecania está construido para costar **0 €/mes** y servir a **un solo usuario: yo**. Eso no es una carencia disimulada, es una restricción elegida, y quiero que se lea como lo que es: la decisión consciente de qué comprar y qué no con el dinero que no hay. Sirve para lo que sirve (mi propio coche y mis manuales) y demuestra dos cosas: que sé construir dentro de un presupuesto y que sé decir en qué punto exacto ese presupuesto deja de ser aceptable.

Cada punto de abajo es una decisión deliberada, y al lado está escrito qué cambiaría con usuarios reales. Las decisiones están documentadas con sus alternativas en los ADRs (ver [ADR-011](docs/adr/011-modelos-gratuitos-y-coste-cero.md), [ADR-009](docs/adr/009-plataformas-despliegue.md) y [ADR-010](docs/adr/010-almacenamiento-objetos-s3.md)).

- **Modelo de IA del chat:** una variante gratuita de OpenRouter, con tope de 5 $ configurado en la clave y elegida por configuración, no por código (hoy `nvidia/nemotron-3-ultra-550b-a55b:free`; cuota de 1.000 peticiones al día). Con usuarios reales: un modelo de pago con SLA, caché de respuestas y control de gasto por usuario. El cambio es una variable de entorno.
- **Embeddings:** `text-embedding-3-small` **de pago**, a propósito. No existe un equivalente gratuito con la misma dimensión y cambiarlo obligaría a migrar la tabla de vectores y recalcular todos los manuales. Coste real: ~0,02 $/millón de tokens, unas cinco diezmilésimas por manual. Con usuarios reales cambiaría solo por privacidad (modelo autoalojado), no por coste.
- **Base de datos:** Neon en nivel gratuito (0,5 GB, escala a cero, 6 horas de restauración puntual) + volcado diario propio a un bucket. Con usuarios reales: plan con restauración a semanas, réplicas de lectura y backups gestionados.
- **Almacenamiento de manuales:** Cloudflare R2 en nivel gratuito (10 GB, sin coste de salida). Con usuarios reales: el mismo servicio, con ciclo de vida de objetos y borrado garantizado por RGPD (hoy el borrado de documentos es una carencia conocida y declarada).
- **Cómputo:** Render en plan gratuito, una sola instancia y arranque en frío de 30 a 90 segundos. Con usuarios reales: instancias dedicadas con autoescalado y sin arranque en frío.
- **Operación:** vigilante propio de `/health/ready` cada 10 minutos, volcado diario de la base y auto-despliegue. Con usuarios reales: métricas, trazas, alertas con SLO y turno de guardia.
- **Esquema de base de datos:** `ddl-auto=update` de Hibernate. Con usuarios reales: Flyway con migraciones versionadas y reversibles (ya está en el roadmap, es el siguiente ticket).
- **Límite de peticiones:** token bucket en memoria, activo solo en el perfil de producción. Con usuarios reales: Redis y cuotas por usuario y plan, porque con varias réplicas el contador en memoria no sirve.
- **Secretos:** variables de entorno del proveedor y un fichero con permisos 600 en el servidor. Con usuarios reales: gestor de secretos con rotación y auditoría de accesos.
- **Autenticación:** JWT propio (HS256) con aislamiento por usuario verificado en cada endpoint. Con usuarios reales: proveedor de identidad, segundo factor y registro de auditoría.

Lo que **no** cambia entre los dos escenarios es lo que de verdad se evalúa aquí: arquitectura por capas con el dominio aislado de la infraestructura, **239 tests** con integración real sobre PostgreSQL, contrato de errores consistente, aislamiento de datos entre usuarios comprobado con tests, verificación en producción de cada cambio, ADRs con alternativas evaluadas y un README que no miente sobre lo que hay. Cambiar de escenario es cambiar de plan y de proveedor; no es rehacer el diseño.

## 📚 Documentación

- [ADR 001: Stack Tecnológico](docs/adr/001-stack-tecnologico.md) — decisión inicial de stack.
- [ADR 002: Seguridad abierta en fase MVP](docs/adr/002-seguridad-abierta-mvp.md) — decisión consciente de `permitAll()` para acelerar validación.
- [ADR 003: Estrategia de almacenamiento de manuales](docs/adr/003-almacenamiento-manuales-disco-local.md) — disco local ahora, Supabase Storage o S3 cuando se despliegue a producción real.
- [ADR 004: Embeddings y procesamiento asíncrono](docs/adr/004-embeddings-y-procesamiento-async.md) — extracción, chunking 512/64 y `@Async` disparado tras el commit.
- [ADR 005: Búsqueda asistida de manuales y descarga selectiva](docs/adr/005-busqueda-asistida-manuales.md) — DuckDuckGo HTML en vez de API de pago, validación SSRF al descargar y errores explícitos por motivo.
- [ADR 006: Chat RAG por vehículo con pgvector](docs/adr/006-chat-rag-por-vehiculo.md) — persistencia de vectores por JDBC nativo, búsqueda por coseno restringida al vehículo y LLM con fuentes verificables.
- [ADR 007: Alertas de mantenimiento por vehículo](docs/adr/007-alertas-mantenimiento.md) — CRUD anidado, lógica de repetitividad y revisión de vencidas.
- [ADR 008: Autenticación JWT y multi-tenencia](docs/adr/008-autenticacion-jwt-multi-tenencia.md) — cada usuario solo ve y toca sus vehículos.
- [ADR 009: Plataformas de despliegue](docs/adr/009-plataformas-despliegue.md) — Render, Neon y dominio propio con HTTPS, con las alternativas descartadas y su coste.
- [ADR 010: Almacenamiento de manuales en objetos (S3/R2)](docs/adr/010-almacenamiento-objetos-s3.md) — el disco del contenedor es efímero; los manuales van a un servicio de objetos.
- [ADR 011: Modelos gratuitos y coste cero como decisión explícita](docs/adr/011-modelos-gratuitos-y-coste-cero.md) — qué se decide por presupuesto y qué por criterio de ingeniería, y cuándo se revisa.
- [docs/HISTORY.md](docs/HISTORY.md) — evolución y decisiones descartadas.
- [docs/METRICS.md](docs/METRICS.md) — qué cubren los tests (no solo cuántos).
- [docs/ROADMAP.md](docs/ROADMAP.md) — plan honesto de fases futuras.
- [SECURITY.md](SECURITY.md) — política de reporte de vulnerabilidades y manejo de secrets.
- [.github/ISSUE_TEMPLATE/](.github/ISSUE_TEMPLATE/) — plantillas para bugs y feature requests.

## 🚀 Cómo ejecutar

### 1. Levantar la base de datos

```bash
docker compose up -d postgres
```

Esto iniciará un contenedor de PostgreSQL con la extensión pgvector (disponible para futuras extensiones de búsqueda vectorial).

### 2. Compilar y ejecutar la aplicación

```bash
mvn spring-boot:run
```

La aplicación estará disponible en `http://localhost:8080`.

### 3. Probar los endpoints

#### Vehículos (CRUD)

- **Crear vehículo**
  ```bash
  curl -X POST http://localhost:8080/api/vehiculos     -H "Content-Type: application/json"     -d '{
      "usuarioId": 1,
      "marca": "Toyota",
      "modelo": "Corolla",
      "anio": 2020,
      "combustible": "GASOLINA",
      "kilometraje": 15000,
      "matricula": "ABC123"
    }'
  ```

- **Obtener vehículo por ID**
  ```bash
  curl http://localhost:8080/api/vehiculos/1
  ```

- **Listar vehículos**
  ```bash
  curl http://localhost:8080/api/vehiculos
  ```

- **Actualizar vehículo**
  ```bash
  curl -X PUT http://localhost:8080/api/vehiculos/1     -H "Content-Type: application/json"     -d '{
      "marca": "Toyota",
      "modelo": "Corolla Hybrid",
      "anio": 2020,
      "combustible": "GASOLINA",
      "kilometraje": 15000,
      "matricula": "ABC123"
    }'
  ```

- **Eliminar vehículo**
  ```bash
  curl -X DELETE http://localhost:8080/api/vehiculos/1
  ```

#### Documentos por vehículo (subida de manuales)

- **Subir manual** (PDF, TXT o DOCX, máximo 25 MB)
  ```bash
  curl -X POST http://localhost:8080/api/vehiculos/1/documentos \
    -F "file=@/ruta/al/manual.pdf"
  ```

- **Listar manuales del vehículo**
  ```bash
  curl http://localhost:8080/api/vehiculos/1/documentos
  ```

- **Descargar un manual**
  ```bash
  curl -O http://localhost:8080/api/vehiculos/1/documentos/5/download
  ```

La implementación actual guarda los archivos detrás de la interfaz `AlmacenamientoArchivos`, con dos implementaciones: disco local en desarrollo (`mecania.storage.tipo=local`, el valor por defecto) y **un servicio de objetos compatible con S3 en producción** (`mecania.storage.tipo=s3`, Cloudflare R2), porque el disco de un contenedor es efímero y en cada redespliegue se perdían los manuales. La descarga se sirve en flujo, sin cargar el fichero entero en memoria. Ver [ADR 010](docs/adr/010-almacenamiento-objetos-s3.md) y [ADR 003](docs/adr/003-almacenamiento-manuales-disco-local.md) (sustituido en producción).

#### Búsqueda asistida de manuales (Fase 3)

Es una función de **búsqueda asistida**: el sistema propone candidatos y solo descarga lo que el usuario acepta.

- **Proponer candidatos** (no descarga nada; `q` es una consulta libre opcional)
  ```bash
  curl "http://localhost:8080/api/vehiculos/1/manuales/candidatos?q=cambio%20de%20aceite"
  ```

- **Importar una URL aceptada** (PDF, TXT o DOCX, máximo 10 MB: esta vía es la que descarga desde internet, así que mantiene un tope más bajo a propósito, ver [ADR 005](docs/adr/005-busqueda-asistida-manuales.md))
  ```bash
  curl -X POST http://localhost:8080/api/vehiculos/1/manuales/importar \
    -H "Content-Type: application/json" \
    -d '{"url":"https://www.ejemplo.es/manuales/toyota-corolla-2018.pdf"}'
  ```

Respuestas de error, con código propio por motivo: `400` URL inválida u host no permitido (protección SSRF: loopback, rangos privados y metadatos de la nube están bloqueados), `413` archivo demasiado grande, `415` tipo no soportado, `502` fallo de red y `503` cuando el buscador externo bloquea la petición (en ese caso la UI invita a pegar la URL a mano). Reimportar la misma URL para el mismo vehículo devuelve `200` con `yaExistia: true` y no vuelve a descargar ni reprocesar el archivo. Ver [ADR 005](docs/adr/005-busqueda-asistida-manuales.md).

#### Chat RAG por vehículo (Fase 4)

El asistente responde usando SOLO los manuales de ese vehículo: vectoriza la pregunta, recupera los fragmentos más parecidos por coseno (pgvector) y responde citando las fuentes. Si no hay manuales relevantes, responde "sin base" sin inventar.

```bash
curl -X POST http://localhost:8080/api/vehiculos/1/chat \
  -H "Content-Type: application/json" \
  -d '{"pregunta":"¿cada cuántos kilómetros se cambia el aceite?"}'
```

Respuesta: `{ "respuesta": "...", "sinBase": false, "fuentes": [ { "documentoId": 5, "posicion": 0, "texto": "...", "similitud": 0.61 } ] }`. Ver [ADR 006](docs/adr/006-chat-rag-por-vehiculo.md).

#### Alertas de mantenimiento (Fase 5)

```bash
curl -X POST http://localhost:8080/api/vehiculos/1/alertas \
  -H "Content-Type: application/json" \
  -d '{"tipo":"ITV","descripcion":"ITV","fecha":"2026-10-01","repetitividad":"ANUAL"}'
```

Listar: `GET /api/vehiculos/1/alertas`. Revisar vencidas (devuelve y desactiva/avanza): `GET /api/vehiculos/1/alertas/vencidas`. Ver [ADR 007](docs/adr/007-alertas-mantenimiento.md).

> Desde la Fase 6 hay autenticación real: `POST /api/auth/register` y `login` (abiertos), el resto de `/api/**` exige un token JWT y cada usuario solo ve/toca sus vehículos; sin token → 401. El ADR-002 (seguridad abierta del MVP) quedó **sustituido** por el [ADR 008](docs/adr/008-autenticacion-jwt-multi-tenencia.md).

## 📖 Aprendizajes clave

Durante la construcción inicial de Mecania, estos fueron aprendizajes concretos:

1. **Hexagonal ligera vs sobreingeniería**: Empezar con capas claras (domain, application, infrastructure, interface) evita que el código se vuelva espagueti incluso en un monorepo pequeño.
2. **Tests que aportan confianza**: Es mejor tener pocos tests que verifiquen comportamientos reales (CRUD completo, manejo de duplicados, respuestas HTTP) que muchos tests mockeados que no prueban nada.
3. **Documentar decisiones desde el inicio**: Un ADR por decisión arquitectónica (por ejemplo, stack o seguridad) es más liviano que un wiki enorme y sirve como punto de entrada para nuevos colaboradores.
4. **Separar lo demostrable de lo production-ready**: En un portfolio está bien dejar ciertas cosas (como login) para fases posteriores, siempre que se documenten explícitamente y no se oculten como limitaciones.
5. **YAGNI en la práctica**: Antes de añadir una dependencia o una capa de abstracción, preguntar si el problema actual realmente la necesita. Muchas veces el JDK o Spring Boot lo resuelven sin complejidad extra.

## 🔓 Transparencia sobre limitaciones

- **Seguridad:** autenticación JWT obligatoria en `/api/**` (excepto registro/login) y multi-tenencia por usuario; sin token → 401. El ADR-002 (seguridad abierta del MVP) quedó sustituido por el ADR-008. El `JWT_SECRET` no tiene valor por defecto: sin la variable, la app falla al arrancar.
- **Frontend:** La interfaz Bootstrap es funcional pero mínima; se centra en demostrar la API, no en habilidades de diseño UI/UX.
- **Búsqueda de manuales y LLM:** la búsqueda de manuales usa el HTML público de DuckDuckGo (ADR 005): puede fallar si el buscador bloquea la petición, y por eso existe el camino alternativo de pegar la URL a mano. El chat RAG (Fase 4) responde solo con los manuales del vehículo vía pgvector + OpenRouter y devuelve las fuentes; si no hay contexto relevante, responde "sin base" en vez de inventar (ADR 006).
- **Alertas de mantenimiento:** la notificación es un log de consola (no hay email/push todavía) y la revisión de vencidas es manual vía endpoint (ADR 007).
- **Autenticación de usuario:** `POST /api/auth/register` y `POST /api/auth/login` devuelven un token JWT (caducidad 24 h); los endpoints protegidos exigen `Authorization: Bearer <token>` (ADR 008).

## 🙏 Créditos

Proyecto diseñado con criterio arquitectónico propio.

--- 

*Última actualización: 2026-09-17*
