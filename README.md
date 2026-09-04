# Mecania

![Java](https://img.shields.io/badge/Java-21-red)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![pgvector](https://img.shields.io/badge/pgvector-enabled-orange)
![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0-lightgrey)
![Tests](https://img.shields.io/badge/tests-12-brightgreen)
![License](https://img.shields.io/badge/license-MIT-yellow)

## 🎯 Qué es Mecania y por qué existe (como pieza de portafolio)

**Mecania** es un **MVP DEMO/PORTFOLIO** creado para demostrar mi capacidad de dirigir agentes de IA sobre Java/Spring Boot y mi habilidad de análisis de negocio en el dominio de mantenimiento vehicular. **NO es un producto para cliente real**, sino un ejemplo de cómo abordar el desarrollo de software cuando se quiere mostrar arquitectura técnica, decisiones de diseño y proceso asistido por IA.

Este proyecto muestra:
- Arquitectura hexagonal limpia con dominio rico (`Vehiculo`, `Documento`, `Fragmento`, `Alerta`).
- Persistencia con PostgreSQL y extensión pgvector para futuras características de RAG.
- API REST completa, testeada y documentada con OpenAPI/Swagger.
- Decisiones arquitectónicas documentadas en forma de ADRs.
- Honestidad intelectual sobre limitaciones (p.ej. seguridad abierta en MVP) y suposiciones.

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
    B -->|HTTP| D[Tavily API (búsqueda de manuales)]
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
- **Búsqueda externa:** Integración con Tavily API (para buscar manuales en internet)
- **LLM:** Integración con modelos de lenguaje vía OpenRouter (para generar respuestas basadas en fragmentos recuperados)
- **Documentación API:** Springdoc OpenAPI (Swagger UI disponible en `/swagger-ui.html`)
- **Build:** Maven 3.9+
- **Tests:** JUnit 5 + Mockito + Testcontainers (PostgreSQL real en pruebas de integración)
- **Contenedores:** Docker + Docker Compose (para PostgreSQL con pgvector)
- **Frontend (post-MVP):** Bootstrap 5.3 (puede evolucionar a Angular/React si se desea mostrar habilidades frontend)

## 📚 Documentación

- [ADR 001: Stack Tecnológico](docs/adr/001-stack-tecnologico.md) — decisión inicial de stack.
- [ADR 002: Seguridad abierta en fase MVP](docs/adr/002-seguridad-abierta-mvp.md) — decisión consciente de `permitAll()` para acelerar validación.
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

> En la fase MVP todos los endpoints están abiertos (`permitAll()`). Ver [ADR 002](docs/adr/002-seguridad-abierta-mvp.md) para detalles.

## 📖 Aprendizajes clave

Durante la construcción inicial de Mecania, estos fueron aprendizajes concretos:

1. **Hexagonal ligera vs sobreingeniería**: Empezar con capas claras (domain, application, infrastructure, interface) evita que el código se vuelva espagueti incluso en un monorepo pequeño.
2. **Tests que aportan confianza**: Es mejor tener pocos tests que verifiquen comportamientos reales (CRUD completo, manejo de duplicados, respuestas HTTP) que muchos tests mockeados que no prueban nada.
3. **Documentar decisiones desde el inicio**: Un ADR por decisión arquitectónica (por ejemplo, stack o seguridad) es más liviano que un wiki enorme y sirve como punto de entrada para nuevos colaboradores.
4. **Separar lo demostrable de lo production-ready**: En un portfolio está bien dejar ciertas cosas (como login) para fases posteriores, siempre que se documenten explícitamente y no se oculten como limitaciones.
5. **YAGNI en la práctica**: Antes de añadir una dependencia o una capa de abstracción, preguntar si el problema actual realmente la necesita. Muchas veces el JDK o Spring Boot lo resuelven sin complejidad extra.

## 🧠 Cómo se construyó — proceso con IA

Este proyecto siguió una metodología de desarrollo asistida por IA rigurosa:

1. **Orquestación principal:** Elías (orquestador Hermes) definió la arquitectura general, tomó decisiones técnicas y supervisó la calidad.
2. **Especialización de subagentes:**
   - `$dev`: Implementación de funcionalidades (API, web, almacenamiento, RAG).
   - `$doc`: Creación y mantenimiento de documentación técnica (specs, ADRs, métricas).
   - `$ideas`: Brainstorming de enfoques y validación de supuestos.
3. **Validación humana continua:** Jorge (el usuario) proporcionó feedback en cada fase clave:
   - Aprobación del stack inicial.
   - Validación del motor de almacenamiento y búsqueda.
   - Revisiones de arquitectura y decisiones de diseño.
   - Verificación de tests y calidad del código.
4. **Flujo de trabajo:**
   - Brainstorming inicial con `$ideas` para definir alcance y enfoque.
   - Diseño arquitectónico y creación de ADRs preliminares.
   - Desarrollo incremental TDD con `$dev` (primero CRUD vehículos, luego almacenamiento de manuales, luego búsqueda Tavily, luego RAG, luego alertas).
   - Documentación paralela con `$doc` (specs, ADRs, métricas).
   - Integración y pruebas end-to-end.
   - Despliegue en staging y producción (Render/Fly.io) cuando corresponda.

Este enfoque permitió construir una base técnica sólida en tiempo limitado, aprovechando la IA para tareas rutinarias (boilerplate, búsqueda de documentación, redacción inicial) y enfocando el esfuerzo humano en decisiones arquitectónicas y validación de negocio.

## 🔓 Transparencia sobre limitaciones

- **Seguridad:** En la fase actual, Mecania corre con seguridad abierta (`permitAll()`) para facilitar pruebas y demos. Esto **NO es apto para producción**. Ver ADR 002 para detalles y plan futuro.
- **Frontend:** La interfaz Bootstrap es funcional pero mínima; se centra en demostrar la API, no en habilidades de diseño UI/UX.
- **Búsqueda Tavily y LLM:** Las integraciones con Tavily y OpenRouter están planeadas pero aún no implementadas. Los placeholders y pruebas usarán mocks o llamadas controladas según corresponda.
- **Autenticación de usuario:** aún no hay endpoints de registro/login; se añadirán cuando sea necesario demostrar manejo de identidad y multi-tenencia.

## 🙏 Créditos

Proyecto diseñado con criterio arquitectónico propio, implementado con asistencia de IA.

--- 

*Última actualización: $(date '+%Y-%m-%d')*
