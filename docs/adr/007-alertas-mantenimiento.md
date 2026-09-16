# ADR 007: Alertas de mantenimiento por vehículo

- **Estado:** Implementado
- **Fecha:** 2026-09-16

## Contexto

La Fase 5 pide recordatorios de mantenimiento (ITV, aceite, frenos, custom)
asociados a un vehículo, con CRUD y una revisión que avise de lo vencido.

## Decisión

1. **Endpoints anidados bajo el vehículo.** En lugar del literal `/api/alertas`
   del roadmap, el CRUD vive en `/api/vehiculos/{vehiculoId}/alertas`, igual
   que documentos, manuales y chat. La pertenencia al vehículo queda
   estructural (no se puede operar sobre la alerta de otro vehículo) y se
   mantiene la convención del resto de la API.
2. **`Repetitividad` en tres valores** (`UNICA`, `MENSUAL`, `ANUAL`) con
   lógica de dominio pura en `RevisorAlertas`: una alerta `UNICA` vencida se
   devuelve y se desactiva; una repetitiva avanza su fecha hasta quedar en el
   futuro, saltando las ocurrencias ya pasadas. Es lógica testeable sin base de
   datos.
3. **Revisión manual por endpoint, no por `@Scheduled`.** `GET
   /api/vehiculos/{vehiculoId}/alertas/vencidas` ejecuta el revisor, persiste
   las mutaciones y devuelve las vencidas. La notificación por ahora es un log
   de consola.
4. **Sin dependencias ni APIs de pago.** Todo es JPA + lógica local.

## Alternativas consideradas

- **`@Scheduled` en arranque**: añade infraestructura de scheduling que el MVP
  no necesita; el endpoint manual cubre la demostración y es determinista para
  los tests.
- **Notificación por email/push/webhook**: sin destinatario real en una pieza
  de portfolio; se deja como mejora en el mismo punto (`AlertaService.vencidas`).
- **`/api/alertas` plano (sin anidar)**: rompía la convención de vehículo y
  obligaba a validar pertenencia a mano en cada endpoint.

## Consecuencias

- La "notificación" es un log de consola; un webhook/email futuro se enchufa en
  el mismo sitio sin tocar el resto.
- Las alertas únicas se desactivan tras avisar (no reaparecen como vencidas).
  Para reactivarlas hay que re-crearlas o actualizarlas.
