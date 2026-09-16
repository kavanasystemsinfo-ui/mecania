# ADR 008 — Autenticación JWT y multi-tenencia

- **Estado:** Aceptada
- **Fecha:** 2026-09-16

## Contexto

Hasta la Fase 5 la API no distinguía usuarios: cualquier cliente podía listar,
crear, modificar o borrar vehículos de cualquiera, y `Vehiculo.usuarioId` se
enviaba en el body de la petición (nada impedía suplantar a otro usuario).

La Fase 6 (autenticación) debe: (1) dar de alta y entrar a los usuarios, (2)
proteger los endpoints, y (3) que cada usuario solo vea y toque SUS datos
(multi-tenencia).

## Decisión

- **JWT firmado con HS256** usando `jjwt` 0.11.5. El token lleva el `sub`
  (email) y el claim `uid` (id de usuario). Se genera en `JwtService` y se
  valida en `JwtAuthenticationFilter` (filtro Spring Security sin estado).
- **Endpoints de auth**: `POST /api/auth/register` (201) y `POST /api/auth/login`
  (200). La contraseña se guarda hasheada con BCrypt (`PasswordEncoder`).
- **Protección**: `/api/auth/**` queda abierto; el resto de `/api/**` exige token
  válido. Un `/api/**` sin token devuelve 401 con cuerpo JSON. La seguridad se
  desactiva con `mecania.auth.enabled=false` (perfil de test de lógica de negocio).
- **Multi-tenencia en el modelo**: `VehiculoRequest` ya no lleva `usuarioId`; el
  propietario lo determina el token. `VehiculoRepository` expone
  `findByIdAndUsuarioId` y `existsByIdAndUsuarioId`, y todos los controllers
  (vehículos + anidados) filtran por el usuario autenticado. Un acceso a un
  vehículo ajeno devuelve 404 (no 403), para no filtrar qué ids existen.
- **Origen de la identidad**: `SecurityUtils.usuarioIdActual()` lee el principal
  del `SecurityContext`. Con auth deshabilitada (solo tests) devuelve el usuario
  demo 1.

## Alternativas consideradas

- **OAuth2/OIDC (login social o proveedor externo)**: descartado por ahora; añade
  infraestructura y no aporta al MVP de portfolio.
- **Sesiones + cookies (stateful)**: descartado; la API es REST y un JWT sin
  estado encaja mejor con el despliegue previsto (front separado / móvil).
- **Spring Security Resource Server (Nimbus)**: equivalente, pero requiere
  `spring-boot-starter-oauth2-resource-server`, que no estaba en el repo offline;
  `jjwt` sí estaba disponible localmente.

## Consecuencias

- Positivas: la API es segura y cada usuario queda aislado; el propietario ya no
  puede suplantarse por el body.
- Negativas: el front (`index.html`) aún no tiene login y queda roto hasta añadir
  la UI de autenticación (paso siguiente de la Fase 6).
- El secreto JWT es SOLO de desarrollo (está en `application.properties`); en
  despliegue debe venir del entorno (`mecania.jwt.secret`).
