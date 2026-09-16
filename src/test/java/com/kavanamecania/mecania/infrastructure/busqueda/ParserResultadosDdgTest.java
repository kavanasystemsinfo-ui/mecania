package com.kavanamecania.mecania.infrastructure.busqueda;

import com.kavanamecania.mecania.domain.busqueda.CandidatoManual;
import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Specs del parseo del HTML de resultados de DuckDuckGo.
 * Sin red: el HTML se construye con la MISMA estructura que sirve DDG
 * (bloques {@code div.result} con {@code a.result__a}, {@code a.result__url}
 * y {@code a.result__snippet}, y enlaces de redirección vía {@code /l/?uddg=}).
 */
class ParserResultadosDdgTest {

    private final ParserResultadosDdg parser = new ParserResultadosDdg();

    private static final String UDDG_TOYOTA =
            "//duckduckgo.com/l/?uddg=https%3A%2F%2Fwww.toyota.com%2Fowners%2Fwarranty%2Downers%2Dmanuals%2Fvehicle%2Fcorolla%2F2018%2F&rut=bd829daa";
    private static final String UDDG_PDF =
            "//duckduckgo.com/l/?uddg=https%3A%2F%2Fcdn.ejemplo.es%2Fmanuales%2Ftoyota%2Dcorolla%2D2018.pdf&rut=0f5eb498";

    @Test
    void devuelve_titulo_url_snippet_y_fuente_de_cada_resultado() {
        String html = pagina(
                bloque(UDDG_TOYOTA, "Toyota Manuals and Warranties | Toyota Owners",
                        "MANUALS &amp; WARRANTIES 2018 Corolla Select your vehicle.",
                        "www.toyota.com/owners/warranty-owners-manuals/vehicle/corolla/2018/"));

        List<CandidatoManual> candidatos = parser.parsear(html, 10);

        assertThat(candidatos).hasSize(1);
        CandidatoManual c = candidatos.get(0);
        assertThat(c.titulo()).isEqualTo("Toyota Manuals and Warranties | Toyota Owners");
        assertThat(c.url()).isEqualTo("https://www.toyota.com/owners/warranty-owners-manuals/vehicle/corolla/2018/");
        assertThat(c.snippet()).isEqualTo("MANUALS & WARRANTIES 2018 Corolla Select your vehicle.");
        assertThat(c.fuente()).isEqualTo("www.toyota.com");
    }

    @Test
    void resuelve_la_url_real_escondida_en_el_redirect_uddg() {
        String html = pagina(bloque(UDDG_PDF, "Manual Toyota Corolla 2018",
                "Manual de usuario completo.", "cdn.ejemplo.es/manuales/toyota-corolla-2018.pdf"));

        List<CandidatoManual> candidatos = parser.parsear(html, 10);

        // Nunca devolvemos el enlace de duckduckgo.com: el usuario debe poder abrir la fuente real.
        assertThat(candidatos.get(0).url())
                .isEqualTo("https://cdn.ejemplo.es/manuales/toyota-corolla-2018.pdf");
        assertThat(candidatos.get(0).url()).doesNotContain("duckduckgo.com");
    }

    @Test
    void marca_los_resultados_que_apuntan_a_un_pdf() {
        String html = pagina(
                bloque(UDDG_PDF, "Manual Toyota Corolla 2018", "Manual en PDF.", "cdn.ejemplo.es/manuales/toyota-corolla-2018.pdf"),
                bloque(UDDG_TOYOTA, "Toyota Owners", "Página de propietarios.", "www.toyota.com/owners/"));

        List<CandidatoManual> candidatos = parser.parsear(html, 10);

        assertThat(candidatos.get(0).pdf()).isTrue();
        assertThat(candidatos.get(0).url()).endsWith(".pdf");
        assertThat(candidatos.get(1).pdf()).isFalse();
    }

    @Test
    void limpia_etiquetas_y_entidades_del_snippet() {
        String html = pagina(bloque(UDDG_TOYOTA, "Toyota <b>Corolla</b>",
                "<b>MANUALS</b> &amp; WARRANTIES &quot;2018&quot;", "www.toyota.com"));

        List<CandidatoManual> candidatos = parser.parsear(html, 10);

        assertThat(candidatos.get(0).titulo()).isEqualTo("Toyota Corolla");
        assertThat(candidatos.get(0).snippet()).isEqualTo("MANUALS & WARRANTIES \"2018\"");
    }

    @Test
    void ignora_bloques_sin_titulo_o_sin_url() {
        String sinTitulo = """
                <div class="result results_links web-result">
                  <div class="result__body">
                    <h2 class="result__title"><a class="result__a" href="%s"></a></h2>
                    <a class="result__snippet" href="%s">Sin título</a>
                  </div>
                </div>
                """.formatted(UDDG_TOYOTA, UDDG_TOYOTA);
        String sinUrl = """
                <div class="result results_links web-result">
                  <div class="result__body">
                    <h2 class="result__title"><a class="result__a">Solo título</a></h2>
                    <a class="result__snippet">Sin url</a>
                  </div>
                </div>
                """;
        String valido = bloque(UDDG_PDF, "Manual válido", "Snippet válido.", "cdn.ejemplo.es/manual.pdf");

        List<CandidatoManual> candidatos = parser.parsear(pagina(sinTitulo, sinUrl, valido), 10);

        assertThat(candidatos).hasSize(1);
        assertThat(candidatos.get(0).titulo()).isEqualTo("Manual válido");
    }

    @Test
    void devuelve_lista_vacia_si_la_pagina_no_tiene_resultados() {
        String html = "<html><body><div class=\"no-results\">No results.</div></body></html>";

        assertThat(parser.parsear(html, 10)).isEmpty();
    }

    @Test
    void devuelve_lista_vacia_con_html_vacio_o_nulo() {
        assertThat(parser.parsear("", 10)).isEmpty();
        assertThat(parser.parsear(null, 10)).isEmpty();
    }

    @Test
    void deduplica_resultados_repetidos_por_url() {
        String html = pagina(
                bloque(UDDG_PDF, "Manual Toyota Corolla 2018", "Primera aparición.", "cdn.ejemplo.es/manuales/toyota-corolla-2018.pdf"),
                bloque(UDDG_PDF, "Manual Toyota Corolla 2018 (duplicado)", "Segunda aparición.", "cdn.ejemplo.es/manuales/toyota-corolla-2018.pdf"));

        List<CandidatoManual> candidatos = parser.parsear(html, 10);

        assertThat(candidatos).hasSize(1);
        assertThat(candidatos.get(0).titulo()).isEqualTo("Manual Toyota Corolla 2018");
    }

    @Test
    void respeta_el_maximo_de_resultados_pedido() {
        String html = pagina(
                bloque(UDDG_PDF, "Uno", "S1", "a.ejemplo.es/1.pdf"),
                bloque(UDDG_TOYOTA, "Dos", "S2", "b.ejemplo.es/2"),
                bloque(UDDG_PDF, "Tres", "S3", "c.ejemplo.es/3.pdf"));

        List<CandidatoManual> candidatos = parser.parsear(html, 2);

        assertThat(candidatos).hasSize(2);
        assertThat(candidatos).extracting(CandidatoManual::titulo).containsExactly("Uno", "Dos");
    }

    @Test
    void acepta_enlaces_absolutos_sin_redireccion() {
        String html = pagina("""
                <div class="result results_links web-result">
                  <div class="result__body">
                    <h2 class="result__title">
                      <a class="result__a" href="https://manua.ls/toyota/corolla-2018/manual">User manual Toyota Corolla (2018)</a>
                    </h2>
                    <a class="result__snippet" href="https://manua.ls/toyota/corolla-2018/manual">608 páginas.</a>
                  </div>
                </div>
                """);

        List<CandidatoManual> candidatos = parser.parsear(html, 10);

        assertThat(candidatos).hasSize(1);
        assertThat(candidatos.get(0).url()).isEqualTo("https://manua.ls/toyota/corolla-2018/manual");
        assertThat(candidatos.get(0).fuente()).isEqualTo("manua.ls");
    }

    @Test
    void ignora_los_anuncios_que_apuntan_al_propio_buscador() {
        // Comprobado contra DDG real: los dos primeros resultados eran anuncios
        // (/y.js?ad_domain=...) y aparecían con "fuente: duckduckgo.com".
        String anuncio = "https://duckduckgo.com/y.js?ad_domain=emanualonline.com&ad_provider=bing";
        String html = pagina(
                bloqueConUrl("https://duckduckgo.com/y.js?ad_domain=emanualonline.com", "Auto Repair Manuals Online", "Anuncio."),
                bloque(UDDG_TOYOTA, "Toyota Manuals and Warranties | Toyota Owners",
                        "MANUALS & WARRANTIES 2018 Corolla", "www.toyota.com/owners/"));

        List<CandidatoManual> candidatos = parser.parsear(html, 10);

        assertThat(candidatos).hasSize(1);
        assertThat(candidatos.get(0).fuente()).isEqualTo("www.toyota.com");
        assertThat(candidatos).extracting(CandidatoManual::url).doesNotContain(anuncio);
    }

    // --- helpers: HTML con la misma estructura que sirve DuckDuckGo ---

    /** Bloque con la URL de redirección que DDG construye para una URL final. */
    private static String bloqueConUrl(String urlReal, String titulo, String snippet) {
        String href = "//duckduckgo.com/l/?uddg="
                + URLEncoder.encode(urlReal, StandardCharsets.UTF_8)
                + "&rut=bd829daa";
        return bloque(href, titulo, snippet, urlReal);
    }

    private static String pagina(String... bloques) {
        return "<html><body><div id=\"links\">" + String.join("\n", bloques) + "</div></body></html>";
    }

    private static String bloque(String href, String titulo, String snippet, String urlVisible) {
        return """
                <div class="result results_links results_links_deep web-result ">
                  <div class="links_main links_deep result__body">
                    <h2 class="result__title">
                      <a rel="nofollow" class="result__a" href="%s">%s</a>
                    </h2>
                    <div class="result__extras">
                      <div class="result__extras__url">
                        <a class="result__url" href="%s">%s</a>
                      </div>
                    </div>
                    <a class="result__snippet" href="%s">%s</a>
                    <div class="clear"></div>
                  </div>
                </div>
                """.formatted(href, titulo, href, urlVisible, href, snippet);
    }
}
