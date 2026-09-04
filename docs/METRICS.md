# METRICS.md

Qué cubren los tests, no solo cuántos.

## Resumen
El proyecto Mecania cuenta con **12 tests automatizados** que cubren los siguientes aspectos críticos del dominio:

### Capa de controlador (VehiculoControllerTest)
- **5 tests**: validan los endpoints REST bajo diferentes condiciones.
  - `GET /api/vehiculos` (listado total y filtrado por usuarioId)
  - `GET /api/vehiculos/{id}` (obtención por ID)
  - `POST /api/vehiculos` (creación exitosa y conflicto por duplicado)
  - `PUT /api/vehiculos/{id}` (actualización)
  - `DELETE /api/vehiculos/{id}` (eliminación)

### Capa de servicio (VehiculoServiceTest)
- **7 tests**: validan la lógica de servicio sin involucrar el layer HTTP.
  - `findAll()` y `findByUsuarioId(Long)`
  - `findById(Long)` con existencia y inexistencia
  - `create(VehiculoRequest)` con validación de duplicado y creación exitosa
  - `update(Long, VehiculoRequest)` con cambios en todos los campos
  - `delete(Long)` con existencia y inexistencia

### Aspectos cubiertos
- **Validación de entrada**: uso de `@Valid` en controller y pruebas de constraint violations (implícitas en tests de creación/actualización).
- **Reglas de negocio**: prevención de duplicados por `(usuarioId, marca, modelo, anio)`.
- **Manejo de excepciones**: conversión de excepciones de dominio a códigos HTTP adecuados (404, 409).
- **Transaccionalidad**: uso de `@Transactional` en métodos de escritura.
- **Integración con Spring Data JPA**: métodos del repository llamados correctamente.
- **Respuesta REST**: códigos de estado, Location header en POST, cuerpo de respuesta mapeado a DTO.

## Qué NO está cubierto actualmente (y por qué)
- **Capa de persistencia directa**: no hay tests que usen `EntityManager` o `native query` porque la capa de repository abstrae esa interacción y ya está testeada vía service.
- **Escenarios de concurrencia**: no se testa aún condiciones de carrera bajo carga alta; se asumirá en fases posteriores si el dominio lo requiere.
- **Integración con pgvector / RAG**: aún no implementado; se añadirá en los tests de esas features cuando se desarrollen.
- **Endpoints de autenticación**: no existen todavía; se cubrirán cuando se implemente login/registro.

## Cómo leer este archivo
Este métrico no pretende ser una métrica de cobertura de línea (aunque esa también es útil), sino una **declaración de intención**: qué aspectos del dominio creemos que están verificados por los tests actuales. Cada vez que se añada una nueva feature, se debe actualizar esta sección con lo que sus tests cubren.
