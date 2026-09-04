# Mecania

Asistente IA para mecánica de coches. Proyecto de portfolio de Jorge Adán (Kavana Systems).

## Stack

- Java 21 + Spring Boot 3.4
- PostgreSQL 16 + pgvector
- Angular 21 (frontend, to be added)
- Docker + Docker Compose

## Getting Started

1. Levantar PostgreSQL: `docker compose up -d` (escucha en `localhost:5433`).
2. Compilar y testear: `mvn clean verify`.
3. Arrancar la app: `mvn spring-boot:run`.
4. Endpoints REST bajo `/api/vehiculos`.

## API de Vehículos

Base URL: `http://localhost:8080/api/vehiculos`

| Método | Path                        | Body                                | Respuesta                                    |
|--------|-----------------------------|-------------------------------------|----------------------------------------------|
| GET    | `/api/vehiculos`            | —                                   | `200` lista de `VehiculoResponse`            |
| GET    | `/api/vehiculos?usuarioId=` | —                                   | `200` lista filtrada por usuario             |
| GET    | `/api/vehiculos/{id}`       | —                                   | `200` `VehiculoResponse` / `404`             |
| POST   | `/api/vehiculos`            | `VehiculoRequest` (JSON, validado)  | `201` con `Location` / `400` / `409`         |
| PUT    | `/api/vehiculos/{id}`       | `VehiculoRequest`                   | `200` / `400` / `404`                        |
| DELETE | `/api/vehiculos/{id}`       | —                                   | `204` / `404`                                |

### `VehiculoRequest` (entrada)

```json
{
  "usuarioId": 1,
  "marca": "Toyota",
  "modelo": "Corolla",
  "anio": 2018,
  "combustible": "GASOLINA",
  "kilometraje": 80000,
  "matricula": "1234ABC"
}
```

`combustible` ∈ `GASOLINA | DIESEL | HIBRIDO | ELECTRICO | GLP | GNC`.

### Errores (formato unificado)

```json
{
  "timestamp": "2026-09-03T19:50:00Z",
  "status": 400,
  "error": "validacion",
  "message": "Datos inválidos",
  "fields": { "marca": "no debe estar vacío" }
}
```

Códigos de error: `vehiculo_no_encontrado` (404), `vehiculo_duplicado` (409), `validacion` (400), `integridad_datos` (409), `error_interno` (500).

### Ejemplo con curl

```bash
# Crear
curl -X POST http://localhost:8080/api/vehiculos \
  -H 'Content-Type: application/json' \
  -d '{"usuarioId":1,"marca":"Toyota","modelo":"Corolla","anio":2018,"combustible":"GASOLINA","kilometraje":80000,"matricula":"1234ABC"}'

# Listar por usuario
curl http://localhost:8080/api/vehiculos?usuarioId=1

# Actualizar
curl -X PUT http://localhost:8080/api/vehiculos/1 \
  -H 'Content-Type: application/json' \
  -d '{"usuarioId":1,"marca":"Toyota","modelo":"Corolla","anio":2019,"combustible":"HIBRIDO","kilometraje":85000,"matricula":"1234ABC"}'

# Eliminar
curl -X DELETE http://localhost:8080/api/vehiculos/1
```

## Tests

- `mvn test` ejecuta los tests unitarios (Surefire): `VehiculoServiceTest` (7) y `VehiculoControllerTest` (5).
- `mvn verify` añade los tests de integración (Failsafe): `VehiculoControllerIT` (3) con H2 en memoria, perfil `test`.

## Vector Store (Optional)

The project includes an optional `DocumentEmbedding` entity and repository for storing vector embeddings of technical manuals, user guides, or FAQ entries. This enables similarity search for retrieving relevant documents when answering user questions about vehicle issues, parts, or procedures.

### How to use
1. Ensure PostgreSQL has the pgvector extension enabled (the `docker-compose.yml` already uses `ankane/pgvector:latest`).
2. The `DocumentEmbedding` entity maps a `double[]` field to a `vector(384)` column (adjust dimension as needed).
3. To add documents:
   - Parse manuals/user guides into text chunks.
   - Generate embeddings using a sentence-transformer model (e.g., `all-MiniLM-L6-v2` from Hugging Face).
   - Save each chunk with its embedding via `DocumentEmbeddingRepository`.
4. To search:
   - Embed the user query with the same model and perform a cosine similarity query against the `embedding` column using pgvector operators (`<=>` for distance).
   - Example native SQL: `SELECT * FROM document_embeddings ORDER BY embedding <=> ? LIMIT 5` where `?` is the query vector.

### Future endpoints
You could add new endpoints under `/api/documentos` or `/api/consultas` to:
- `POST /api/documentos` for ingesting a text chunk with its embedding.
- `GET /api/consultas?pregunta=...` that embeds the question, searches the vector store, and returns the top matches.

Note: This is optional scaffolding; the core vehicle management API works without it.
