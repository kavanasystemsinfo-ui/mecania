package com.kavanamecania.mecania.infrastructure.descarga;

import com.kavanamecania.mecania.domain.descarga.ArchivoDescargado;
import com.kavanamecania.mecania.domain.descarga.DescargaException;
import com.kavanamecania.mecania.domain.descarga.MotivoDescarga;
import com.kavanamecania.mecania.domain.descarga.ValidadorDestinoDescarga;
import com.kavanamecania.mecania.domain.model.Documento;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Specs del descargador HTTP contra un servidor local real (JDK HttpServer).
 * Cubre: nombre y tipo deducidos, límite de tamaño, tipos no soportados,
 * errores del servidor, redirecciones y validación de cada salto (SSRF).
 */
class HttpDescargadorUrlTest {

    private static final long MAX_BYTES = 1024 * 1024;

    private HttpServer server;
    private String base;
    private final List<URI> urisValidadas = new ArrayList<>();
    private Function<String, Respuesta> responder;

    private record Respuesta(int status, Map<String, String> cabeceras, byte[] cuerpo) {
        static Respuesta ok(String contentType, String cuerpo) {
            return new Respuesta(200, Map.of("Content-Type", contentType), cuerpo.getBytes(StandardCharsets.UTF_8));
        }

        static Respuesta ok(String contentType, String cuerpo, String contentDisposition) {
            Map<String, String> h = new LinkedHashMap<>();
            h.put("Content-Type", contentType);
            h.put("Content-Disposition", contentDisposition);
            return new Respuesta(200, h, cuerpo.getBytes(StandardCharsets.UTF_8));
        }

        static Respuesta redirige(String destino) {
            return new Respuesta(302, Map.of("Location", destino), new byte[0]);
        }

        static Respuesta estado(int status) {
            return new Respuesta(status, Map.of("Content-Type", "text/html"), "error".getBytes(StandardCharsets.UTF_8));
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                Respuesta r = responder.apply(exchange.getRequestURI().getPath());
                if (r == null) {
                    r = Respuesta.estado(404);
                }
                r.cabeceras().forEach((k, v) -> exchange.getResponseHeaders().add(k, v));
                long len = r.cuerpo().length == 0 ? -1 : r.cuerpo().length;
                exchange.sendResponseHeaders(r.status(), len);
                if (len > 0) {
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(r.cuerpo());
                    }
                }
            } finally {
                exchange.close();
            }
        });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    /** Descargador con el guardián permisivo: los tests hablan con un servidor local. */
    private HttpDescargadorUrl descargador(long maxBytes) {
        return new HttpDescargadorUrl(uri -> urisValidadas.add(uri), maxBytes, 10, 3);
    }

    @Test
    void descarga_un_pdf_y_devuelve_nombre_tipo_y_bytes() {
        responder = ruta -> Respuesta.ok("application/pdf", "%PDF-1.4 contenido del manual");
        String url = base + "/manual-corolla-2018.pdf";

        ArchivoDescargado archivo = descargador(MAX_BYTES).descargar(url);

        assertThat(archivo.nombreArchivo()).isEqualTo("manual-corolla-2018.pdf");
        assertThat(archivo.tipo()).isEqualTo(Documento.TipoDocumento.PDF);
        assertThat(archivo.contenido()).isEqualTo("%PDF-1.4 contenido del manual".getBytes(StandardCharsets.UTF_8));
        assertThat(archivo.urlFinal()).isEqualTo(url);
    }

    @Test
    void usa_el_nombre_del_content_disposition_si_el_servidor_lo_indica() {
        responder = ruta -> Respuesta.ok("application/pdf", "%PDF-1.4 x",
                "attachment; filename=\"manual-oficial-corolla.pdf\"");
        String url = base + "/descarga?id=42";

        ArchivoDescargado archivo = descargador(MAX_BYTES).descargar(url);

        assertThat(archivo.nombreArchivo()).isEqualTo("manual-oficial-corolla.pdf");
        assertThat(archivo.tipo()).isEqualTo(Documento.TipoDocumento.PDF);
    }

    @Test
    void deduce_el_tipo_por_la_extension_si_el_content_type_es_generico() {
        responder = ruta -> Respuesta.ok("application/octet-stream", "texto del manual");
        String url = base + "/documentos/manual-ibiza.docx";

        ArchivoDescargado archivo = descargador(MAX_BYTES).descargar(url);

        assertThat(archivo.tipo()).isEqualTo(Documento.TipoDocumento.DOCX);
        assertThat(archivo.nombreArchivo()).isEqualTo("manual-ibiza.docx");
    }

    @Test
    void acepta_texto_plano() {
        responder = ruta -> Respuesta.ok("text/plain", "notas del taller");

        ArchivoDescargado archivo = descargador(MAX_BYTES).descargar(base + "/notas.txt");

        assertThat(archivo.tipo()).isEqualTo(Documento.TipoDocumento.TXT);
    }

    @Test
    void rechaza_content_types_no_soportados() {
        responder = ruta -> Respuesta.ok("text/html", "<html>no es un manual</html>");

        assertThatThrownBy(() -> descargador(MAX_BYTES).descargar(base + "/pagina"))
                .isInstanceOf(DescargaException.class)
                .hasMessageContaining("no soportado")
                .satisfies(e -> assertThat(((DescargaException) e).motivo())
                        .isEqualTo(MotivoDescarga.TIPO_NO_SOPORTADO));
    }

    @Test
    void rechaza_descargas_que_superan_el_tamano_maximo() {
        responder = ruta -> Respuesta.ok("application/pdf", "x".repeat(4096));

        assertThatThrownBy(() -> descargador(512).descargar(base + "/enorme.pdf"))
                .isInstanceOf(DescargaException.class)
                .satisfies(e -> assertThat(((DescargaException) e).motivo())
                        .isEqualTo(MotivoDescarga.DEMASIADO_GRANDE));
    }

    @Test
    void rechaza_la_descarga_si_el_servidor_responde_con_error() {
        responder = ruta -> Respuesta.estado(404);

        assertThatThrownBy(() -> descargador(MAX_BYTES).descargar(base + "/no-existe.pdf"))
                .isInstanceOf(DescargaException.class)
                .hasMessageContaining("404")
                .satisfies(e -> assertThat(((DescargaException) e).motivo())
                        .isEqualTo(MotivoDescarga.ERROR_RED));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "file:///etc/passwd",
            "ftp://ejemplo.es/manual.pdf",
            "javascript:alert(1)",
            "  ",
            "no-es-una-url"
    })
    void rechaza_urls_invalidas_o_con_esquema_no_http(String url) {
        assertThatThrownBy(() -> descargador(MAX_BYTES).descargar(url))
                .isInstanceOf(DescargaException.class)
                .satisfies(e -> assertThat(((DescargaException) e).motivo())
                        .isEqualTo(MotivoDescarga.URL_INVALIDA));
    }

    @Test
    void rechaza_url_nula() {
        assertThatThrownBy(() -> descargador(MAX_BYTES).descargar(null))
                .isInstanceOf(DescargaException.class)
                .satisfies(e -> assertThat(((DescargaException) e).motivo())
                        .isEqualTo(MotivoDescarga.URL_INVALIDA));
    }

    @Test
    void valida_tambien_los_hosts_privados_con_el_guardian_real() {
        HttpDescargadorUrl real = new HttpDescargadorUrl(new ValidadorHostsPublicos(), MAX_BYTES, 5, 3);
        responder = ruta -> Respuesta.ok("application/pdf", "%PDF-1.4");

        assertThatThrownBy(() -> real.descargar("http://169.254.169.254/latest/meta-data/"))
                .isInstanceOf(DescargaException.class)
                .satisfies(e -> assertThat(((DescargaException) e).motivo())
                        .isEqualTo(MotivoDescarga.HOST_NO_PERMITIDO));
    }

    @Test
    void sigue_una_redireccion_y_devuelve_la_url_final() {
        responder = ruta -> switch (ruta) {
            case "/ir" -> Respuesta.redirige(base + "/manual.pdf");
            case "/manual.pdf" -> Respuesta.ok("application/pdf", "%PDF-1.4 real");
            default -> null;
        };

        ArchivoDescargado archivo = descargador(MAX_BYTES).descargar(base + "/ir");

        assertThat(archivo.urlFinal()).isEqualTo(base + "/manual.pdf");
        assertThat(archivo.tipo()).isEqualTo(Documento.TipoDocumento.PDF);
    }

    @Test
    void valida_cada_salto_de_la_redireccion_no_solo_la_url_inicial() {
        responder = ruta -> switch (ruta) {
            case "/ir" -> Respuesta.redirige(base + "/manual.pdf");
            case "/manual.pdf" -> Respuesta.ok("application/pdf", "%PDF-1.4 real");
            default -> null;
        };

        descargador(MAX_BYTES).descargar(base + "/ir");

        // El destino de la redirección también pasa por el guardián: si no,
        // un redirect a 169.254.169.254 saltaría la protección SSRF.
        assertThat(urisValidadas).extracting(URI::getPath)
                .containsExactly("/ir", "/manual.pdf");
    }

    @Test
    void rechaza_demasiadas_redirecciones() {
        responder = ruta -> Respuesta.redirige(base + "/ir");

        assertThatThrownBy(() -> descargador(MAX_BYTES).descargar(base + "/ir"))
                .isInstanceOf(DescargaException.class)
                .hasMessageContaining("redirecciones")
                .satisfies(e -> assertThat(((DescargaException) e).motivo())
                        .isEqualTo(MotivoDescarga.ERROR_RED));
    }

    @Test
    void informa_con_claridad_si_no_hay_conexion() throws IOException {
        // Puerto reservado y cerrado inmediatamente: conexión rechazada.
        int puertoCerrado;
        try (java.net.ServerSocket s = new java.net.ServerSocket(0)) {
            puertoCerrado = s.getLocalPort();
        }

        assertThatThrownBy(() -> descargador(MAX_BYTES).descargar("http://127.0.0.1:" + puertoCerrado + "/manual.pdf"))
                .isInstanceOf(DescargaException.class)
                .hasMessageContaining("No se pudo descargar")
                .satisfies(e -> assertThat(((DescargaException) e).motivo())
                        .isEqualTo(MotivoDescarga.ERROR_RED));
    }

    @Test
    void ignora_un_content_disposition_que_intenta_escapar_de_la_carpeta() {
        responder = ruta -> Respuesta.ok("application/pdf", "%PDF-1.4 x",
                "attachment; filename=\"../../../etc/cron.d/pwned.pdf\"");
        String url = base + "/manual.pdf";

        ArchivoDescargado archivo = descargador(MAX_BYTES).descargar(url);

        // El nombre propuesto por el servidor no puede traer componentes de ruta.
        assertThat(archivo.nombreArchivo()).isEqualTo("pwned.pdf");
    }

    @Test
    void explica_que_el_origen_bloquea_las_descargas_cuando_responde_403() {
        // w3.org (y otros) devuelven 403 a clientes automatizados: el mensaje
        // debe decirle al usuario que puede subir el archivo a mano.
        responder = ruta -> Respuesta.estado(403);

        assertThatThrownBy(() -> descargador(MAX_BYTES).descargar(base + "/manual.pdf"))
                .isInstanceOf(DescargaException.class)
                .hasMessageContaining("403")
                .hasMessageContaining("rechaza las descargas automatizadas")
                .satisfies(e -> assertThat(((DescargaException) e).motivo())
                        .isEqualTo(MotivoDescarga.ERROR_RED));
    }
}
