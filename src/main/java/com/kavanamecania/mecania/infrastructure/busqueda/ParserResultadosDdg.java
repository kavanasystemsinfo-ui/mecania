package com.kavanamecania.mecania.infrastructure.busqueda;

import com.kavanamecania.mecania.domain.busqueda.CandidatoManual;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Extrae los resultados del HTML de DuckDuckGo (endpoint {@code html.duckduckgo.com}).
 *
 * <p>DDG no devuelve la URL real en el enlace: la envuelve en
 * {@code /l/?uddg=<url codificada>}. Aquí se resuelve para que el usuario vea y
 * acepte la fuente real, nunca el intermediario.</p>
 *
 * <p>Es puro (sin red) para poder probar el parseo con HTML fijo.</p>
 */
public class ParserResultadosDdg {

    /**
     * @param html          HTML de la página de resultados (puede ser null)
     * @param maxResultados tope de candidatos a devolver
     * @return candidatos en el orden en que aparecen, sin URLs repetidas
     */
    public List<CandidatoManual> parsear(String html, int maxResultados) {
        if (html == null || html.isBlank() || maxResultados <= 0) {
            return List.of();
        }

        Document documento = Jsoup.parse(html);
        Map<String, CandidatoManual> porUrl = new LinkedHashMap<>();

        for (Element enlace : documento.select("a.result__a")) {
            String url = resolverUrl(enlace.attr("href"));
            String titulo = enlace.text().trim();
            if (url == null || titulo.isEmpty() || esDelBuscador(url) || porUrl.containsKey(url)) {
                continue;
            }
            porUrl.put(url, new CandidatoManual(titulo, url, snippetDe(enlace), hostDe(url), esPdf(url)));
            if (porUrl.size() >= maxResultados) {
                break;
            }
        }

        return new ArrayList<>(porUrl.values());
    }

    private static String snippetDe(Element enlace) {
        Element bloque = enlace.closest(".result");
        if (bloque == null) {
            return "";
        }
        Element snippet = bloque.selectFirst("a.result__snippet");
        return snippet == null ? "" : snippet.text().trim();
    }

    /** Devuelve la URL real del resultado, o null si no es utilizable. */
    private static String resolverUrl(String href) {
        if (href == null || href.isBlank()) {
            return null;
        }
        String candidata = href.startsWith("//") ? "https:" + href : href;

        if (candidata.contains("uddg=")) {
            String decodificada = parametroUddg(candidata);
            return decodificada != null && esHttp(decodificada) ? decodificada : null;
        }
        return esHttp(candidata) ? candidata : null;
    }

    private static String parametroUddg(String url) {
        String consulta;
        try {
            consulta = URI.create(url).getRawQuery();
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (consulta == null) {
            return null;
        }
        for (String parametro : consulta.split("&")) {
            if (parametro.startsWith("uddg=")) {
                // El '+' de una URL no es un espacio: preservarlo antes de decodificar.
                String valor = parametro.substring("uddg=".length()).replace("+", "%2B");
                return URLDecoder.decode(valor, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static boolean esHttp(String url) {
        String minusculas = url.toLowerCase();
        return minusculas.startsWith("http://") || minusculas.startsWith("https://");
    }

    /**
     * Descarta los enlaces que siguen apuntando al propio buscador: son anuncios
     * ({@code /y.js?ad_domain=...}) o redirecciones que no se pudieron resolver.
     * Comprobado contra DDG real: sin este filtro los dos primeros candidatos
     * eran anuncios con "fuente: duckduckgo.com".
     */
    private static boolean esDelBuscador(String url) {
        String host = hostDe(url);
        return host.equals("duckduckgo.com") || host.endsWith(".duckduckgo.com");
    }

    private static String hostDe(String url) {
        try {
            String host = URI.create(url).getHost();
            return host == null ? "" : host.toLowerCase();
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    private static boolean esPdf(String url) {
        String sinParametros = url.split("[?#]")[0].toLowerCase();
        return sinParametros.endsWith(".pdf");
    }
}
