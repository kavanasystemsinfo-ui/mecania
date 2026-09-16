package com.kavanamecania.mecania.infrastructure.busqueda;

import com.kavanamecania.mecania.domain.busqueda.BusquedaException;
import com.kavanamecania.mecania.domain.busqueda.CandidatoManual;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Specs del buscador contra un servidor local que imita a DuckDuckGo.
 * Verifica el parseo, la codificación de la consulta, los headers de
 * navegador y, sobre todo, que una página anti-bot NO se confunda con
 * "no hay resultados" (honestidad: el usuario debe saber que falló).
 */
class DuckDuckGoBuscadorManualesTest {

    private static final String UDDG_PDF =
            "//duckduckgo.com/l/?uddg=https%3A%2F%2Fcdn.ejemplo.es%2Fmanuales%2Ftoyota%2Dcorolla%2D2018.pdf&rut=0f5eb498";

    private HttpServer server;
    private String endpoint;
    private Function<String, Resultado> responder;
    private String ultimaQuery;
    private String ultimoUserAgent;
    private String ultimoAcceptLanguage;

    private record Resultado(int status, String html) {
    }

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/html/", exchange -> {
            ultimaQuery = exchange.getRequestURI().getRawQuery();
            ultimoUserAgent = exchange.getRequestHeaders().getFirst("User-Agent");
            ultimoAcceptLanguage = exchange.getRequestHeaders().getFirst("Accept-Language");
            Resultado r = responder.apply(ultimaQuery);
            byte[] cuerpo = r.html().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(r.status(), cuerpo.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(cuerpo);
            }
            exchange.close();
        });
        server.start();
        // El buscador es un endpoint de configuración (no lo elige el usuario):
        // no pasa por el guardián SSRF, así que puede apuntar al servidor local.
        endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/html/";
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void busca_y_devuelve_los_candidatos_parseados() {
        responder = q -> new Resultado(200, pagina(bloque("Manual Toyota Corolla 2018", "Manual completo en PDF.")));

        List<CandidatoManual> candidatos = buscador(10).buscar("Toyota Corolla 2018 manual");

        assertThat(candidatos).hasSize(1);
        assertThat(candidatos.get(0).titulo()).isEqualTo("Manual Toyota Corolla 2018");
        assertThat(candidatos.get(0).url()).isEqualTo("https://cdn.ejemplo.es/manuales/toyota-corolla-2018.pdf");
        assertThat(candidatos.get(0).pdf()).isTrue();
    }

    @Test
    void devuelve_lista_vacia_si_la_pagina_es_valida_pero_no_hay_resultados() {
        responder = q -> new Resultado(200, "<html><body><div class=\"no-results\">No results.</div></body></html>");

        assertThat(buscador(10).buscar("marca inexistente")).isEmpty();
    }

    @Test
    void codifica_la_consulta_y_envia_headers_de_navegador() {
        responder = q -> new Resultado(200, pagina(bloque("Manual", "Snippet.")));

        buscador(10).buscar("Toyota Corolla 2018 manual pdf");

        assertThat(URLDecoder.decode(ultimaQuery, StandardCharsets.UTF_8))
                .isEqualTo("q=Toyota Corolla 2018 manual pdf");
        // Sin UA de navegador DuckDuckGo devuelve su página anti-bot (202).
        assertThat(ultimoUserAgent).contains("Mozilla");
        assertThat(ultimoAcceptLanguage).isNotBlank();
    }

    @Test
    void lanza_busqueda_exception_si_el_buscador_devuelve_la_pagina_anti_bot() {
        // DDG responde 202 con el challenge de imágenes cuando detecta un bot.
        responder = q -> new Resultado(202, """
                <html><body>
                  <div id="anomaly-modal">
                    <input type="checkbox" class="anomaly-modal__check" name="image-check_80dbb8e3">
                    <img id="image-80dbb8e3" src="../assets/anomaly/images/challenge/80dbb8e3.jpg">
                  </div>
                </body></html>
                """);

        assertThatThrownBy(() -> buscador(10).buscar("Toyota Corolla 2018 manual"))
                .isInstanceOf(BusquedaException.class)
                .hasMessageContaining("bloqueado");
    }

    @Test
    void lanza_busqueda_exception_si_el_buscador_responde_con_error_http() {
        responder = q -> new Resultado(500, "<html><body>Server error</body></html>");

        assertThatThrownBy(() -> buscador(10).buscar("Toyota Corolla"))
                .isInstanceOf(BusquedaException.class)
                .hasMessageContaining("500");
    }

    @Test
    void lanza_busqueda_exception_si_no_hay_conexion() throws IOException {
        int puertoCerrado;
        try (java.net.ServerSocket s = new java.net.ServerSocket(0)) {
            puertoCerrado = s.getLocalPort();
        }
        DuckDuckGoBuscadorManuales buscador =
                new DuckDuckGoBuscadorManuales("http://127.0.0.1:" + puertoCerrado + "/html/", 10, 5);

        assertThatThrownBy(() -> buscador.buscar("Toyota Corolla"))
                .isInstanceOf(BusquedaException.class)
                .hasMessageContaining("No se pudo contactar");
    }

    @Test
    void respeta_el_maximo_de_resultados_configurado() {
        responder = q -> new Resultado(200, pagina(
                bloqueConUrlReal("https://a.ejemplo.es/manual-1.pdf", "Manual 1", "S1"),
                bloqueConUrlReal("https://b.ejemplo.es/manual-2.pdf", "Manual 2", "S2"),
                bloqueConUrlReal("https://c.ejemplo.es/manual-3.pdf", "Manual 3", "S3")));

        assertThat(buscador(2).buscar("Toyota Corolla")).hasSize(2);
    }

    private DuckDuckGoBuscadorManuales buscador(int maxResultados) {
        return new DuckDuckGoBuscadorManuales(endpoint, maxResultados, 5);
    }

    private static String pagina(String... bloques) {
        return "<html><body><div id=\"links\">" + String.join("\n", bloques) + "</div></body></html>";
    }

    private static String bloque(String titulo, String snippet) {
        return bloqueConHref(UDDG_PDF, titulo, snippet);
    }

    /** Bloque con la URL de redirección que DDG construye para una URL final. */
    private static String bloqueConUrlReal(String urlReal, String titulo, String snippet) {
        String href = "//duckduckgo.com/l/?uddg=" + URLEncoder.encode(urlReal, StandardCharsets.UTF_8) + "&rut=0f5eb498";
        return bloqueConHref(href, titulo, snippet);
    }

    private static String bloqueConHref(String href, String titulo, String snippet) {
        return """
                <div class="result results_links results_links_deep web-result ">
                  <div class="links_main links_deep result__body">
                    <h2 class="result__title">
                      <a rel="nofollow" class="result__a" href="%s">%s</a>
                    </h2>
                    <a class="result__snippet" href="%s">%s</a>
                  </div>
                </div>
                """.formatted(href, titulo, href, snippet);
    }
}
