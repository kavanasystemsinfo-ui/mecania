# ADR 001: Stack Tecnológico

**Estado:** Aceptado  
**Fecha:** 2026-09-03  
**Contexto:** Necesitamos elegir un stack tecnológico para el proyecto Mecania que permita a Jorge demostrar su capacidad de dirigir agentes de IA sobre cualquier tecnología, y que sea lo suficientemente relevante para el mercado laboral español (especialmente ofertas como la de M&GT Consulting que pide Java, Spring Boot, PostgreSQL). Además, el proyecto debe ser realizable en el tiempo estimado del MVP (3 semanas) y tener una curva de aprendizaje razonable dada la experiencia previa de Jorge en TypeScript/Node/NestJS/React.

**Decisión:** Utilizamos el siguiente stack:

- **Backend:** Java 21 + Spring Boot 3.4
- **Persistencia:** PostgreSQL 16 con extensión pgvector para búsqueda vectorial (RAG)
- **Autenticación:** Spring Security con JWT
- **Documentación API:** Springdoc OpenAPI (Swagger UI)
- **Build:** Maven 3.9
- **Tests:** JUnit 5 + Mockito + Testcontainers
- **Contenedores:** Docker + Docker Compose (para PostgreSQL)
- **Frontend (post-MVP):** Angular 21 standalone (para reutilizar conocimiento existente)

**Consecuencias:**

- *Positivas:* 
  - El stack Java + Spring Boot es muy demandado en el mercado español y coincide con ofertas como la de M&GT.
  - PostgreSQL es una base de datos robusta y ampliamente usada.
  - La extensión pgvector permite implementar RAG sin necesidad de una base de datos vectorial separada, reduciendo complejidad y coste.
  - Spring Boot proporciona un ecosistema maduro con starter para web, data, seguridad, etc.
  - El uso de Lombok reduce el código boilerplate.
  - Testcontainers permite pruebas de integración reales con PostgreSQL.
  - El frontend se pospone para el MVP, pero el conocimiento de Angular de Jorge puede reutilizarse posteriormente sin perder tiempo en aprender un nuevo framework frontend ahora.

- *Negativas:*
  - Jorge tiene menos experiencia directa en Java que en TypeScript, pero su metodología de dirigir agentes de IA le permite aprender rápidamente.
  - El tiempo de compilación de Java es mayor que de TypeScript interpretado, pero esto es aceptable para un proyecto de portfolio.
  - La extensión pgvector requiere una versión específica de PostgreSQL (usamos la imagen ankane/pgvector:pg16 en Docker Compose).

**Alternativas consideradas:**

1. **TypeScript + NestJS + PostgreSQL + pgvector (usando node-postgres y pgvector vía JavaScript):** 
   - Ventaja: Jorge ya conoce este stack.
   - Desventaja: No cumple el objetivo de demostrar capacidad en un nuevo stack (Java) y no es tan atractivo para ofertas Java específicas.

2. **Python + FastAPI + PostgreSQL + pgvector (usando psycopg y extensiones de Python):**
   - Ventaja: Python es fácil de aprender y tiene buenas bibliotecas para IA.
   - Desventaja: Menos demandado en ofertas enterprise españolas comparado con Java Spring Boot para roles de backend.

3. **Utilizar una base de datos vectorial separada (Pinecone, Weaviate, Qdrant):**
   - Ventaja: Posiblemente mejor rendimiento y características avanzadas.
   - Desventaja: Añade complejidad de infraestructura, coste adicional, y depende de servicios externos. Para un MVP y portfolio, mantener todo en una sola base de datos (PostgreSQL + pgvector) es más sencillo y demostrable.

**Nota:** Esta decisión será revisada si surgen problemas técnicos insalvables con pgvector o si cambia el objetivo del proyecto.