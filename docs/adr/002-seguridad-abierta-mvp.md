# ADR 002: Seguridad abierta en fase MVP

**Estado:** Aceptado   
**Fecha:** 2026-09-04   

## Contexto
Necesitamos validar rápidamente el flujo completo de Mecania (CRUD vehículos, subida manuales, búsqueda Tavily, RAG por vehículo, alertas) sin que la fricción de login obstaculice las pruebas de integración y demos internas. El proyecto es portfolio y aún no hay usuarios reales; el objetivo es demostrar arquitectura y decisiones técnicas, no seguridad per se.

## Decisión
Mantenemos `spring.security.authorizeHttpRequests.anyRequest().permitAll()` y desactivamos CSRF (`csrf -> csrf.disable()`) en `SecurityConfig.java`.  
No se implementa autenticación ni autorización en esta fase.  
El modelo `Usuario` y su servicio con BCrypt quedan como base futura, pero sin endpoints ni filtros activos.

## Alternativas consideradas
1. **JWT desde el inicio**: requeriría endpoints de registro/login, middleware de validación en cada request y gestión de tokens en tests. Añade complejidad inicial que ralentiza la validación de features core.
2. **Spring Security con usuario en memoria fijo**: más seguro que `permitAll()` pero sigue siendo de demostración y obliga a mantener credenciales hardcodeadas.
3. **OAuth2/Login social**: fuera de alcance para MVP y añade dependencias externas no relevantes para el objetivo de portfolio.

## Consecuencias
### Positivas
- Permite probar todos los endpoints inmediatamente con curl o Postman sin manejo de tokens.
- Reduce código de bootstrap y configuración de seguridad innecesaria para validar features de negocio.
- El modelo `Usuario` ya está listo para cuando se decida activar seguridad (solo hay que crear los endpoints y el filter chain).

### Negativas
- Cualquiera puede acceder a la API si se expone públicamente. **No es apto para producción sin cambios**.
- Se debe documentar explícitamente esta limitación para evitar suposiciones erróneas.

## Nota de revisión
Esta decisión se revisará cuando se decida pasar a fase de autenticación real. En ese momento se creará un nuevo ADR que sustituirá a este y definirá el flujo de login elegido (probablemente JWT con endpoints `/api/auth/*`).
