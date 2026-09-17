# ADR-011: Modelos gratuitos y coste cero como decisión explícita

**Estado:** ✅ Implementado · **Fecha:** 2026-09-17 · **Sustituye a:** nada · **Relacionado:** ADR-004 (embeddings), ADR-006 (chat RAG), ADR-009 (plataformas)

## Contexto

Mecania es una pieza de portafolio con **un único usuario real: su autor**. El objetivo de coste es 0 €/mes y está declarado desde el ADR-009. El producto necesita dos llamadas a un proveedor de IA en cada ciclo de uso:

1. **Embeddings**, para indexar cada manual que se sube (una llamada por fragmento).
2. **LLM de chat**, para redactar la respuesta a partir de los fragmentos recuperados.

Ambas cuestan dinero por uso y la segunda es la que puede crecer sin control: no es solo el precio por token, es el modelo de reserva del proveedor (una petición sin `max_tokens` reserva el máximo del modelo). Eso ya provocó un fallo real en producción: con saldo pequeño, el proveedor devolvía `402 Payment Required` y el chat caía con `503`.

El problema que resuelve este ADR no es técnico, es de encuadre: **separar lo que se decide por falta de presupuesto de lo que se decide por criterio de ingeniería**, y dejarlo escrito para que quien lea el repositorio no confunda una limitación del entorno con una carencia del diseño.

## Decisión

1. **El chat usa una variante gratuita** de OpenRouter, elegida por configuración (`mecania.chat.model`, variable `MECANIA_CHAT_MODEL`). Hoy: `nvidia/nemotron-3-ultra-550b-a55b:free`.
2. **Los embeddings se quedan en un modelo de pago barato** (`text-embedding-3-small`) a propósito. No existe equivalente gratuito con la misma dimensión en OpenRouter, y cambiarlo obligaría a migrar la tabla de vectores y recalcular todos los manuales ya indexados. El coste real es un redondeo (ver consecuencias).
3. **La elección de modelo vive en configuración, no en código.** Cambiar de proveedor o de modelo es una variable de entorno y un redespliegue, no una modificación de código: las capas de dominio dependen de las interfaces `LlmService` y `EmbeddingService`, y las implementaciones son sustituibles.
4. **El encuadre se documenta de cara al lector** en el README (sección «Cómo está construido y cómo lo construiría con presupuesto»), con las partidas que cambiarían con usuarios reales y las que no cambiarían en absoluto.

## Alternativas evaluadas

| Alternativa | A favor | En contra | Veredicto |
|---|---|---|---|
| Modelo de pago (`gpt-4o-mini`) para el chat | Mejor calidad, sin cuota diaria, sin 429 del proveedor de origen | Coste variable con una clave de facturación sin techo; innecesario para un solo usuario | Descartado para el uso actual; documentado como la elección con usuarios reales |
| Modelo autoalojado (Ollama en el VPS) | Coste 0 y los datos no salen del servidor | Exige CPU/RAM que el plan gratuito no tiene y complica el despliegue y la operación | Aplazado; es la ruta natural si algún día hay datos sensibles |
| Embeddings locales (ONNX, 384 dimensiones) | Coste 0 y sin dependencia de proveedor | Cambia la dimensión del vector: migración de esquema y recálculo de todos los manuales; añade dependencia pesada a la imagen | Descartado por relación coste/beneficio: el modelo de pago cuesta céntimos al año |
| Otros modelos gratuitos del catálogo | Coste 0 | Dos devolvían contenido vacío (el servicio los rechaza como «respuesta vacía»), uno exponía su razonamiento interno en la respuesta y otro venía limitado por el proveedor de origen (`429`) | Descartados tras probarlos contra el caso real (pregunta dentro y fuera del manual) |

El criterio de selección del modelo gratuito no fue «el más grande», sino dos pruebas concretas del comportamiento que necesita un RAG: **responder en español citando solo el manual** y **decir «no tengo esa información» cuando la pregunta se sale del contexto**. El elegido hace las dos en 1,3-1,7 s.

## Consecuencias

**Positivas**

- Coste de IA prácticamente 0 €: el chat no cuesta y los embeddings son un redondeo.
- No hay una clave de facturación expuesta a gasto ilimitado: además del modelo gratuito, la clave tiene un tope de gasto configurado.
- El código no depende del proveedor elegido: cambiar de modelo es configuración.
- La decisión queda escrita, lo que demuestra tanto el criterio como la implementación.

**Negativas y límites asumidos (dichos por delante)**

- **Cuota de peticiones diaria** en las variantes gratuitas (1.000/día en esta cuenta, que compró créditos históricamente; 50/día en cuentas sin compras). Consultable en `GET /api/v1/key` → `free_model_daily_requests`.
- **Disponibilidad y latencia variables**: los modelos gratuitos pueden devolver `429` del proveedor de origen. Mitigación pendiente: lista de modelos de respaldo (fallback) en la misma configuración.
- **Los modelos gratuitos pueden registrar los prompts.** Consecuencia directa: Mecania no debe usarse con datos personales reales mientras el chat vaya por esta vía.
- **Un saldo negativo en la cuenta rechaza incluso los modelos gratuitos**, así que hay que mantener saldo positivo aunque el consumo sea céntimos.
- **Los embeddings sí cuestan dinero**, aunque sea despreciable: 0,02 $/millón de tokens, es decir, unas cinco diezmilésimas de dólar por un manual de 10 KB; mil manuales, unos cinco céntimos.

## Señal de revisión

Este ADR se revisa si ocurre cualquiera de estas cuatro cosas: (a) el proyecto pasa a tener usuarios distintos de su autor; (b) entran datos personales o de terceros; (c) la cuota diaria o los `429` dejan de cubrir el uso real; (d) el coste de embeddings deja de ser un redondeo.

En los cuatro casos **el cambio es de configuración y de plan de plataforma, no de arquitectura**: se cambia el modelo, se sube la plataforma de los niveles gratuitos y el resto del sistema (dominio, tests, contrato de errores, multi-tenencia) sigue igual. Esa asimetría es, precisamente, el objetivo de documentarla.
