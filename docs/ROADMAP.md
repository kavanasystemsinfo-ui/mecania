# ROADMAP.md

Plan honesto de lo que viene, lo que se pospone y lo que no haremos.

## Próximas fases (orden estimado)

### Fase 1: Almacenamiento y gestión de manuales ✅
- ~~Subida de archivos PDF/TXT/DOCX desde el vehículo del usuario.~~ ✅ implementado 2026-09-04
- ~~Almacenamiento en disco o blob (por definir).~~ ✅ disco local, abstracto vía `AlmacenamientoArchivos`
- ~~Entidad `Documento` y `Fragmento` reactivadas con relación a `Vehiculo`.~~ ✅
- ~~Endpoints: `POST /api/documentos`, `GET /api/documentos/{vehiculoId}`, etc.~~ ✅ `POST/GET /api/vehiculos/{vehiculoId}/documentos[/...]`
- ~~Tests unitarios y de integración.~~ ✅ 14 tests nuevos (10 service + 4 storage)
- ~~ADR correspondiente (elección de almacenamiento: disco local vs S3 vs base64 en BD).~~ ✅ ADR 003

### Fase 2: Procesamiento y embeddings (vector store)
- ~~Extracción de texto de PDF/TXT (PDFBox o Tika).~~ ✅ PDFBox 3 (PDF), POI (DOCX) y lectura directa (TXT) tras la interfaz `TextExtractor`
- ~~Chunking inteligente (párrafos o secciones).~~ ✅ ventana deslizante 512/64 (`SlidingWindowChunker`)
- ~~Generación de embeddings mediante modelo local o API (ej. OpenRouter + modelo de embeddings).~~ ✅ `OpenRouterEmbeddingService` (`text-embedding-3-small`), disparado en background
- ~~Almacenamiento en columna `vector` de PostgreSQL vía pgvector.~~ ✅ hecho en Fase 4: tabla auxiliar `fragmento_embeddings` por JDBC nativo (Hibernate no mapea `vector`; ver ADR 006)
- Endpoint interno para generar vectorstore por vehículo. ⏳ **no se hará**: el procesamiento ya se dispara solo al subir el manual; un endpoint manual no aporta nada
- ~~Tests: verificación de que embeddings se generan y se guardan.~~ ✅ unitarios (embedding, chunking, extractores) + integración con fallo a mitad de documento

### Fase 3: Búsqueda asistida y descarga selectiva ✅
- ~~Endpoint `POST /api/tavily/buscar`...~~ ❌ **descartado** (2026-09-06): Tavily consume saldo del titular y Mecania es portfolio. Ver ADR 005.
- ~~Frontend sencillo para que el usuario revise y acepte/rechace cada candidato.~~ ✅ pestaña Documentos: consulta opcional, listado de candidatos con fuente real y botón "Importar"
- ~~Descarga automática solo de los aceptados.~~ ✅ `GET /api/vehiculos/{id}/manuales/candidatos` (propone, no descarga) + `POST /api/vehiculos/{id}/manuales/importar` (descarga y registra)
- ~~Tests de la lógica de filtro y descarga (mock del buscador para evitar llamadas externas en CI).~~ ✅ parseo con HTML fijo, buscador contra servidor local, descargador contra servidor local y validación SSRF
- Búsqueda sobre el HTML público de DuckDuckGo con Jsoup (coste 0, sin credenciales). Si bloquea, se responde 503 explicando que se puede pegar la URL a mano.

### Fase 4: Chat RAG especialista por vehículo ✅
- ~~Endpoint `POST /api/chat` que recibe mensaje y `vehiculoId`.~~ ✅ `POST /api/vehiculos/{vehiculoId}/chat` (2026-09-16)
- ~~Recupera fragmentos relevantes de pgvector (similarity search) limitados a ese vehículo.~~ ✅ coseno `<=>` con filtro `vehiculo_id` y umbral de similitud
- ~~Construye prompt con contexto + mensaje y lo envía a LLM (ej. vía OpenRouter).~~ ✅ `OpenRouterLlmService` (chat/completions, gpt-4o-mini)
- ~~Devuelve respuesta solo basada en los manuales del vehículo (no mezcla info de otros modelos).~~ ✅ prompt de sistema + "sin base" sin llamar al LLM
- ~~Tests de relevancia y de que no se filtra información de otros vehículos.~~ ✅ 4 tests de servicio + restricción por `vehiculo_id` en la query (ver ADR 006)

### Fase 5: Recordatorios y alertas de mantenimiento ✅
- ~~Entidad `Alerta` (tipo: ITV, aceite, frenos, custom) asociada a `Vehiculo`.~~ ✅ (2026-09-16)
- ~~Campos: descripción, fecha, repetitividad (única, mensual, anual), activa.~~ ✅ enum `Repetitividad` (UNICA/MENSUAL/ANUAL) + `activa`
- ~~Endpoints CRUD de `/api/alertas`.~~ ✅ anidados bajo `/api/vehiculos/{id}/alertas` (consistente con el resto, ver ADR 007)
- ~~Servicio sencillo que revisa periódicamente y puede notificar.~~ ✅ `GET .../alertas/vencidas` + `RevisorAlertas` (desactiva únicas, avanza repetitivas); notificación por log de consola (webhook futuro)
- ~~UI sencilla en frontend para crear/ver alertas.~~ ✅ pestaña "Alertas" en el modal del vehículo

### Fase 6: Autenticación y multi-tenencia ✅
- ~~Endpoints `/api/auth/register` y `/api/auth/login` que devuelven JWT.~~ ✅ (2026-09-16, ADR 008)
- ~~Filtro Spring Security que valida JWT y extrae `usuarioId`.~~ ✅ `JwtAuthenticationFilter` + `SecurityUtils`
- ~~Todos los endpoints protegidos excepto `/api/auth/*`.~~ ✅ `/api/auth/**` abierto, resto de `/api/**` exige token (`mecania.auth.enabled`)
- ~~Tests de flujos de autenticación y autorización.~~ ✅ `JwtServiceTest`, `AuthServiceTest`, `AuthControllerIT`, `MultiTenenciaIT`
- ~~ADR que documente la elección (JWT vs session, secret management, expiración).~~ ✅ ADR 008
- ~~UI de login en el front (index.html).~~ ✅ overlay Entrar/Crear cuenta, sesión en localStorage, token en todas las llamadas y botón Salir.

### Fase 7: Despliegue y monitoreo
- Pipeline GitHub Actions que ejecuta `mvn verify` y despliega a un entorno de staging (Render, Fly.io o similares).
- Health checks endpoints (`/actuator/health`).
- Logging estructurado y rotación de logs.
- Documentación de pasos para desplegar en producción.

## Lo que NO haremos (por ahora o nunca)
- **APIs de búsqueda de pago** (Tavily, AIsa, Brave Search): la búsqueda de manuales usa el HTML público de DuckDuckGo con Jsoup (Fase 3, ADR 005) para no consumir saldo del titular del proyecto.
- **Motor de inferencia local de LLMs**: manteneremos la integración vía API (OpenRouter) para evitar complejidad de GPU y licencias en el MVP.
- **Base de datos vectorial externa** (Pinecone, Weaviate): usaremos PostgreSQL + pgvector para reducir movilidad de datos y dependencias externas.
- **Frontend avanzado** (SSR, PWA, WebSockets): el frontend actual es suficiente para demostrar la API; se puede mejorar en iteraciones posteriores si el portfolio lo requiere.
- **Cobro o monetización**: el proyecto es puramente de portfolio; no se añadirán pasarelas de pago ni planes de suscripción.
- **Soporte para navegadores obsoletos**: nos enfocamos en navegadores modernos (Chrome/Firefox/Safari últimas 2 versiones).

## Cómo usar este roadmap
Este documento es una guía honesta, no un compromiso rígido. Las fases pueden reordenarse, combinarse o dividirse según surjan aprendizajes durante el desarrollo. Lo importante es mantener la transparencia sobre qué se está construyendo, qué se pospone y qué se descarta intencionalmente.
