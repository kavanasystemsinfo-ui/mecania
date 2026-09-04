# ROADMAP.md

Plan honesto de lo que viene, lo que se pospone y lo que no haremos.

## Próximas fases (orden estimado)

### Fase 1: Almacenamiento y gestión de manuales (actual)
- Subida de archivos PDF/TXT/DOCX desde el vehículo del usuario.
- Almacenamiento en disco o blob (por definir).
- Entidad `Documento` y `Fragmento` reactivadas con relación a `Vehiculo`.
- Endpoints: `POST /api/documentos`, `GET /api/documentos/{vehiculoId}`, etc.
- Tests unitarios y de integración.
- ADR correspondiente (elección de almacenamiento: disco local vs S3 vs base64 en BD).

### Fase 2: Procesamiento y embeddings (vector store)
- Extracción de texto de PDF/TXT (PDFBox o Tika).
- Chunking inteligente (párrafos o secciones).
- Generación de embeddings mediante modelo local o API (ej. OpenRouter + modelo de embeddings).
- Almacenamiento en columna `vector` de PostgreSQL vía pgvector.
- Endpoint interno para generar vectorstore por vehículo.
- Tests: verificación de que embeddings se generan y se guardan.

### Fase 3: Asistente Tavily y descarga selectiva
- Endpoint `POST /api/tavily/buscar` que recibe marca/modelo/anio y devuelve lista de candidatos (título, URL, fuente, snippet).
- Frontend sencillo para que el usuario revise y acepte/rechace cada candidato.
- Descarga automática solo de los aceptados y almacenado como documentos del vehículo.
- Tests de la lógica de filtro y descarga (mock de Tavily opcional para evitar llamadas externas en CI).

### Fase 4: Chat RAG especialista por vehículo
- Endpoint `POST /api/chat` que recibe mensaje y `vehiculoId`.
- Recupera fragmentos relevantes de pgvector (similarity search) limitados a ese vehículo.
- Construye prompt con contexto + mensaje y lo envía a LLM (ej. vía OpenRouter).
- Devuelve respuesta solo basada en los manuales del vehículo (no mezcla info de otros modelos).
- Tests de relevancia y de que no se filtra información de otros vehículos.

### Fase 5: Recordatorios y alertas de mantenimiento
- Entidad `Alerta` (tipo: ITV, aceite, frenos, custom) asociada a `Vehiculo`.
- Campos: descripción, fecha, repetitividad (única, mensual, anual), activa.
- Endpoints CRUD de `/api/alertas`.
- Servicio sencillo que revisa periódicamente (por ahora, al inicio de la app o mediante endpoint manual) y puede notificar (por consola o futuro webhook).
- UI sencilla en frontend para crear/ver alertas.

### Fase 6: Autenticación y multi-tenencia
- Endpoints `/api/auth/register` y `/api/auth/login` que devuelven JWT.
- Filtro Spring Security que valida JWT y extrae `usuarioId`.
- Todos los endpoints protegidos excepto `/api/auth/*` y documentación OpenAPI.
- Tests de flujos de autenticación y autorización.
- ADR que documente la elección (JWT vs session, secret management, expiración).

### Fase 7: Despliegue y monitoreo
- Pipeline GitHub Actions que ejecuta `mvn verify` y despliega a un entorno de staging (Render, Fly.io o similares).
- Health checks endpoints (`/actuator/health`).
- Logging estructurado y rotación de logs.
- Documentación de pasos para desplegar en producción.

## Lo que NO haremos (por ahora o nunca)
- **Motor de inferencia local de LLMs**: manteneremos la integración vía API (OpenRouter) para evitar complejidad de GPU y licencias en el MVP.
- **Base de datos vectorial externa** (Pinecone, Weaviate): usaremos PostgreSQL + pgvector para reducir movilidad de datos y dependencias externas.
- **Frontend avanzado** (SSR, PWA, WebSockets): el frontend actual es suficiente para demostrar la API; se puede mejorar en iteraciones posteriores si el portfolio lo requiere.
- **Cobro o monetización**: el proyecto es puramente de portfolio; no se añadirán pasarelas de pago ni planes de suscripción.
- **Soporte para navegadores obsoletos**: nos enfocamos en navegadores modernos (Chrome/Firefox/Safari últimas 2 versiones).

## Cómo usar este roadmap
Este documento es una guía honesta, no un compromiso rígido. Las fases pueden reordenarse, combinarse o dividirse según surjan aprendizajes durante el desarrollo. Lo importante es mantener la transparencia sobre qué se está construyendo, qué se pospone y qué se descarta intencionalmente.
