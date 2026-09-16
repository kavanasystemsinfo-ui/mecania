package com.kavanamecania.mecania.infrastructure.busqueda;

import com.kavanamecania.mecania.domain.busqueda.BuscadorManuales;
import com.kavanamecania.mecania.domain.busqueda.BusquedaException;
import com.kavanamecania.mecania.domain.busqueda.CandidatoManual;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * Buscador de manuales sobre el endpoint HTML de DuckDuckGo.
 *
 * <p>Se eligió scraping del HTML público en lugar de una API de búsqueda de pago
 * (Tavily/AIsa) para no consumir saldo del titular del portfolio: ver ADR 005.</p>
 *
 * <p>DDG sirve una página anti-bot (HTTP 202) cuando detecta un cliente que no
 * parece un navegador, así que se envían cabeceras de navegador y esa página se
 * traduce a {@link BusquedaException}: jamás a una lista vacía, porque "no hay
 * resultados" y "la búsqueda falló" son cosas distintas para el usuario.</p>
 */
@Component
public class DuckDuckGoBuscadorManuales implements BuscadorManuales {

    private static final String USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    private static final String MARCADOR_ANTI_BOT = "anomaly-modal";

    private final String endpoint;
    private final int maxResultados;
    private final Duration timeout;
    private final HttpClient http;
    private final ParserResultadosDdg parser = new ParserResultadosDdg();

    public DuckDuckGoBuscadorManuales(
            @Value("${mecania.busqueda.endpoint:https://html.duckduckgo.com/html/}") String endpoint,
            @Value("${mecania.busqueda.max-resultados:10}") int maxResultados,
            @Value("${mecania.busqueda.timeout-segundos:20}") int timeoutSegundos) {
        this.endpoint = endpoint;
        this.maxResultados = maxResultados;
        this.timeout = Duration.ofSeconds(timeoutSegundos);
        this.http = HttpClient.newBuilder().connectTimeout(this.timeout).build();
    }

    @Override
    public List<CandidatoManual> buscar(String consulta) {
        String url = endpoint + "?q=" + URLEncoder.encode(consulta, StandardCharsets.UTF_8);

        HttpResponse<String> respuesta;
        try {
            HttpRequest peticion = HttpRequest.newBuilder(URI.create(url))
                    .timeout(timeout)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
                    .GET()
                    .build();
            respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new BusquedaException("No se pudo contactar con el buscador externo: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusquedaException("Búsqueda interrumpida antes de recibir respuesta", e);
        }

        // La detección anti-bot va ANTES del código HTTP: DDG responde 202 (no 200)
        // con su challenge, y "nos han bloqueado" es más útil que "HTTP 202".
        String html = respuesta.body() == null ? "" : respuesta.body();
        if (html.contains(MARCADOR_ANTI_BOT)) {
            throw new BusquedaException("El buscador externo nos ha bloqueado por tráfico automatizado"
                    + " (protección anti-bot). Reintenta en unos minutos o importa el manual pegando su URL a mano.");
        }

        if (respuesta.statusCode() != 200) {
            throw new BusquedaException("El buscador externo respondió HTTP " + respuesta.statusCode()
                    + ". Puedes importar el manual pegando su URL a mano.");
        }

        return parser.parsear(html, maxResultados);
    }
}
