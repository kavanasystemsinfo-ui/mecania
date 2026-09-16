# ADR 005: Búsqueda asistida de manuales y descarga selectiva

**Estado:** Aceptado  
**Fecha:** 2026-09-16  

## Contexto

Las fases 1 y 2 resolvieron subir manuales y procesarlos (extracción, chunking,
embeddings), pero el usuario sigue teniendo que **encontrar** el manual por su
cuenta y subirlo a mano. Ese es justo el dolor que Mecania dice resolver: la
documentación está dispersa en webs de fabricantes, portales de manuales y foros.

La Fase 3 se planteó inicialmente como "Asistente Tavily": una llamada a la API
de búsqueda de Tavily para obtener candidatos y descargar los aceptados. Esa
propuesta se descartó el 2026-09-06 por un motivo de coste: Tavily (y AIsa)
consumen saldo del titular del proyecto, y Mecania es una pieza de portfolio sin
presupuesto de operación. Quemar saldo del usuario para una demo no es aceptable
si existe una alternativa razonable gratuita (regla del kit de ingeniería:
auditoría de coste de APIs externas antes de proponerlas).

Queda además un problema de honestidad de producto: una búsqueda web **no
garantiza** que el resultado sea el manual correcto del vehículo. El sistema no
debe presentar candidatos como si fueran el manual oficial ni descargar nada sin
que el usuario lo acepte.

## Decisión

### Fuente de búsqueda: HTML público de DuckDuckGo (scraping con Jsoup)
- **Endpoint**: `https://html.duckduckgo.com/html/?q=<consulta>` (versión sin
  JavaScript ni API key).
- **Parseo**: Jsoup sobre los bloques `div.result` (`a.result__a` para título y
  enlace, `a.result__snippet` para el fragmento).
- **URL real**: DDG no expone la URL de destino directamente; la envuelve en
  `/l/?uddg=<url codificada>`. Se resuelve el parámetro `uddg` para que el
  usuario vea y acepte la **fuente real**, nunca el intermediario.
- **Coste**: 0 € y sin credenciales que rotar. Es una dependencia de un HTML
  ajeno y puede romperse; se asume y se documenta.

### Detección de bloqueo anti-bot
- DDG responde `202` con una página de challenge (`class="anomaly-modal__check"`)
  cuando detecta tráfico automatizado (comprobado durante el desarrollo: una
  petición `curl` sin cabeceras de navegador la recibe).
- Se envían cabeceras de navegador (`User-Agent`, `Accept`, `Accept-Language`),
  lo que en las pruebas devolvió resultados reales (HTTP 200).
- Si aun así llega el challenge, se lanza `BusquedaException` → HTTP 503 con
  mensaje explícito. **Nunca se devuelve una lista vacía para disimular el
  fallo**: "no hay resultados" y "la búsqueda está bloqueada" son cosas
  distintas y el usuario debe poder distinguirlas.

### Descarga selectiva (nunca automática)
- Ninguna búsqueda descarga ni persiste nada. La descarga ocurre solo cuando el
  usuario acepta un candidato o pega una URL a mano (`POST .../manuales/importar`).
- Esto convierte la función en "búsqueda **asistida**": el sistema propone, el
  usuario decide. Es también lo que permite documentarla sin prometer magia.

### Descargador con validación de destino (SSRF) y límites
La URL la elige el usuario, así que `HttpDescargadorUrl` (solo JDK, sin
dependencias nuevas) aplica:
- **Solo http/https**: cualquier otro esquema (`file://`, `ftp://`,
  `javascript:`) se rechaza con `URL_INVALIDA`.
- **Host público validado en cada salto**: las redirecciones se siguen a mano
  (`HttpClient.Redirect.NEVER`) para poder validar el destino de cada salto. Con
  el modo automático, un `302` a `169.254.169.254` (metadatos de la nube) sería
  invisible. La comprobación la hace `ValidadorHostsPublicos`, que rechaza
  loopback, rangos privados, link-local, CGNAT, IPv6 unique-local y falla en
  cerrado si el DNS no se puede verificar.
- **Tope de redirecciones** (3 por defecto) y **tope de bytes** (10 MB, el mismo
  límite que la subida manual), aplicado durante la lectura.
- **Solo tipos soportados**: PDF, TXT y DOCX, deducidos por `Content-Type` y, si
  el servidor no lo informa, por la extensión.
- **Nombre saneado**: se toma el `filename` de `Content-Disposition` o el último
  segmento de la URL y se reduce a nombre base, descartando componentes de ruta.

### Errores explícitos con código HTTP por motivo
`DescargaException` lleva un `MotivoDescarga` y `GlobalExceptionHandler` lo
traduce: `URL_INVALIDA`/`HOST_NO_PERMITIDO` → 400, `TIPO_NO_SOPORTADO` → 415,
`DEMASIADO_GRANDE` → 413, `ERROR_RED` → 502. Un fallo de red no se disfraza de
400 ni de 500 genérico.

### Trazabilidad y deduplicación
- `documentos.origen_url` (nullable) guarda la URL de origen de los manuales
  importados; null en las subidas manuales. La UI la muestra como enlace, así el
  usuario ve de dónde salió cada archivo.
- Reimportar la misma URL para el mismo vehículo devuelve el documento existente
  con `yaExistia=true` (HTTP 200 en lugar de 201) y **no** vuelve a descargar ni
  a reprocesar.

### Un solo pipeline de procesamiento
El manual importado se guarda con el mismo `AlmacenamientoArchivos` y dispara el
mismo `DocumentoSubidoEvent` (listener `AFTER_COMMIT`, ADR 004) que la subida
manual. No hay dos caminos de procesamiento que puedan divergir.

## Alternativas consideradas

### Búsqueda
- **Tavily API (diseño original)**: resultados de calidad y sin parseo, pero
  consume saldo del titular. Descartado por coste (decisión consciente, no
  despiste).
- **AIsa / Perplexity Sonar**: mismo problema de saldo y, además, resumen
  generado en lugar de fuentes verificables.
- **Google Programmable Search (CSE)**: capa gratuita limitada y requiere API
  key + motor configurado; más fricción operativa que el HTML público.
- **API oficial de DuckDuckGo (Instant Answer)**: probada, devuelve
  `Results: []` para consultas de manuales (es una API de respuestas, no de
  búsqueda web). Descartada.
- **Bing/Brave Search APIs**: requieren key y plan de pago.

### Descarga
- **`HttpClient` con redirecciones automáticas**: una línea menos, pero
  imposibilita validar cada salto → descartado por SSRF.
- **Librería de scraping/navegador headless**: sobredimensionado para un MVP;
  añade peso al contenedor sin aportar valor.
- **Guardar el resultado de la búsqueda como documento "pendiente"**: contradice
  la decisión de no descargar nada sin aceptación explícita.

## Consecuencias

### Positivas
- Coste 0 y sin credenciales nuevas.
- El usuario ve la fuente antes de aceptar, y solo se descarga lo que aprueba.
- Rango de hosts peligrosos cerrado (SSRF) con tests que lo fijan.
- Reimportar la misma URL no duplica documentos ni reprocesa embeddings.
- Un único pipeline de procesamiento para subida manual e importación.

### Negativas
- La búsqueda depende de un HTML de terceros que puede cambiar o bloquear. Si
  DDG endurece el anti-bot, la función degrada a "pegar la URL a mano" (que
  sigue existiendo) y el mensaje 503 lo explica.
- El scraping de un tercero tiene una zona gris de términos de uso; se asume
  conscientemente en un proyecto de portfolio sin uso comercial, y se documenta
  aquí para que no se descubra como sorpresa.
- El parser no puntúa relevancia: los candidatos se listan en el orden del
  buscador, sin garantizar que el primero sea el correcto.

## Plan de migración cuando se decida desplegar
1. Cambiar a una API de búsqueda con contrato (Brave Search o similar) si hay
   presupuesto, manteniendo la interfaz `BuscadorManuales` (el cambio queda
   aislado en la infraestructura).
2. Añadir caché de búsquedas por vehículo para reducir peticiones y exposición al
   anti-bot.
3. Valorar puntuación/heurística de relevancia (año, extensión, dominio del
   fabricante) si los usuarios reportan ruido.

## Nota de revisión
Se revisará si aparece un ADR 006 sobre autenticación (Fase 6): hoy la importación
está abierta (`permitAll()`, ADR 002) y cualquiera puede disparar descargas. Antes
de exponer esto a producción real hace falta autenticación y, muy probablemente,
límite de tasa por usuario.
