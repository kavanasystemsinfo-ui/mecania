# Mecania

Asistente IA para mecánica de coches. Proyecto portfolio para demostrar habilidades en Java/Spring Boot, PostgreSQL y diseño de APIs REST.

## Características

- API REST para gestión de vehículos (`/api/vehiculos`)
- Seguridad básica con Spring Security (login mediante usuario en memoria)
- Documentación automática de API con Springdoc OpenAPI (Swagger UI disponible en `/swagger-ui.html`)
- Persistencia con Spring Data JPA y PostgreSQL
- Tests unitarios y de integración
- Dockerizado para PostgreSQL

## Requisitos

- Java 21
- Maven 3.8+
- Docker y Docker Compose (para PostgreSQL)

## Instrucciones de uso

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
  ```
  POST /api/vehiculos
  Content-Type: application/json

  {
    "usuarioId": 1,
    "marca": "Toyota",
    "modelo": "Corolla",
    "anio": 2020,
    "combustible": "GASOLINA",
    "kilometraje": 15000,
    "matricula": "ABC123"
  }
  ```

- **Obtener vehículo por ID**
  ```
  GET /api/vehiculos/{id}
  ```

- **Listar vehículos**
  ```
  GET /api/vehiculos
  ```

- **Actualizar vehículo**
  ```
  PUT /api/vehiculos/{id}
  Content-Type: application/json

  {
    "usuarioId": 1,
    "marca": "Toyota",
    "modelo": "Corolla Hybrid",
    "anio": 2020,
    "combustible": "GASOLINA",
    "kilometraje": 15000,
    "matricula": "ABC123"
  }
  ```

- **Eliminar vehículo**
  ```
  DELETE /api/vehiculos/{id}
  ```

### 4. Documentación de la API

Una vez la aplicación esté corriendo, visite:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### 5. Detener todo

```bash
docker compose down
```

Y presione `Ctrl+C` en la terminal donde esté corriendo `mvn spring-boot:run`.

## Notas de diseño

- El proyecto está estructurado en capas típicas de una aplicación Spring Boot:
  - `controller`: endpoints REST
  - `service`: lógica de negocio
  - `model`: entidades JPA
  - `repository`: interfaces de persistencia
  - `application`: DTOs y servicios de aplicación
  - `domain`: excepciones y modelos de dominio puro
  - `infrastructure`: implementaciones específicas de repositorios
- Se utiliza Lombok para reducir código boilerplate.
- Las pruebas usan Testcontainers para PostgreSQL en memoria durante el fase de test.
- La seguridad está configurada con un usuario en memoria (credenciales generadas en cada arranque, visibles en el log).

## Extensiones futuras (ideas para demostrar habilidades avanzadas)

Estos son ejemplos de lo que se podría añadir para mostrar especialización en IA y sistemas multiagente:

1. **Búsqueda vectorial de manuales**: Añadir una entidad `DocumentEmbedding` con campo `vector(384)` para almacenar embeddings de fragmentos de manuales técnicos, y permitir búsquedas de similitud para responder preguntas sobre averías, mantenimiento, etc.

2. **Agente especialista en mecánica**: Implementar un servicio RAG (Retrieval-Augmented Generation) que, dado un síntoma, busque en la base de conocimiento vectorial los fragmentos más relevantes y genere una respuesta usando un LLM.

3. **Endpoint de consulta inteligente**: `/api/consultas/averia` que reciba una descripción de problema y devuelva posibles causas, pasos de diagnóstico y piezas sugeridas.

4. **Integración con agentes externos**: Diseñar el sistema de forma que pueda orquestar subagentes especializados (por ejemplo, uno para electricidad, otro para transmisión, etc.) usando patrones de mensajería o colas.

Estas extensiones se pueden añadir posteriormente sin afectar la estructura básica del proyecto, la cual permanece limpia y enfocada en demostrar competencias backend sólidas.