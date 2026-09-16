package com.kavanamecania.mecania.infrastructure.descarga;

import com.kavanamecania.mecania.domain.descarga.ArchivoDescargado;
import com.kavanamecania.mecania.domain.descarga.DescargaException;
import com.kavanamecania.mecania.domain.descarga.DescargadorUrl;
import com.kavanamecania.mecania.domain.descarga.MotivoDescarga;
import com.kavanamecania.mecania.domain.descarga.ValidadorDestinoDescarga;
import com.kavanamecania.mecania.domain.model.Documento;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Descargador HTTP con las protecciones que exige una URL elegida por el usuario:
 * solo http/https, host público validado en cada salto, tope de redirecciones,
 * tope de bytes leídos y solo tipos de documento soportados.
 *
 * <p>Usa {@code java.net.http} del JDK: no añade dependencias. Las redirecciones
 * se siguen a mano ({@code Redirect.NEVER}) precisamente para poder validar cada
 * destino antes de conectarse: con el modo automático el segundo salto sería
 * invisible.</p>
 */
public class HttpDescargadorUrl implements DescargadorUrl {

    private static final String USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private static final Map<String, Documento.TipoDocumento> TIPOS_POR_CONTENT_TYPE = Map.of(
            "application/pdf", Documento.TipoDocumento.PDF,
            "application/x-pdf", Documento.TipoDocumento.PDF,
            "text/plain", Documento.TipoDocumento.TXT,
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", Documento.TipoDocumento.DOCX);

    private static final Set<Integer> CODIGOS_REDIRECCION = Set.of(301, 302, 303, 307, 308);

    private final ValidadorDestinoDescarga validador;
    private final long maxBytes;
    private final int maxRedirecciones;
    private final Duration timeout;
    private final HttpClient http;

    public HttpDescargadorUrl(ValidadorDestinoDescarga validador, long maxBytes,
                              int timeoutSegundos, int maxRedirecciones) {
        this.validador = validador;
        this.maxBytes = maxBytes;
        this.maxRedirecciones = maxRedirecciones;
        this.timeout = Duration.ofSeconds(timeoutSegundos);
        this.http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(this.timeout)
                .build();
    }

    @Override
    public ArchivoDescargado descargar(String urlCruda) {
        URI destino = validarUrl(urlCruda);

        for (int salto = 0; salto <= maxRedirecciones; salto++) {
            validador.validar(destino);
            HttpResponse<InputStream> respuesta = enviar(destino);

            if (CODIGOS_REDIRECCION.contains(respuesta.statusCode())) {
                String location = respuesta.headers().firstValue("Location").orElse(null);
                cerrar(respuesta);
                if (location == null || location.isBlank()) {
                    throw new DescargaException(MotivoDescarga.ERROR_RED,
                            "El servidor redirigió sin indicar destino: " + destino);
                }
                destino = destino.resolve(location.trim());
                continue;
            }

            if (respuesta.statusCode() != 200) {
                cerrar(respuesta);
                throw new DescargaException(MotivoDescarga.ERROR_RED,
                        "No se pudo descargar " + destino + ": el servidor respondió HTTP " + respuesta.statusCode()
                                + pistaBloqueo(respuesta.statusCode()));
            }

            String contentType = respuesta.headers().firstValue("Content-Type").orElse(null);
            try (InputStream cuerpo = respuesta.body()) {
                byte[] contenido = leerConLimite(cuerpo, destino);
                String nombre = nombreArchivo(respuesta, destino);
                return new ArchivoDescargado(nombre, deducirTipo(contentType, nombre, destino),
                        contenido, contentType, destino.toString());
            } catch (IOException e) {
                throw new DescargaException(MotivoDescarga.ERROR_RED,
                        "Fallo al leer la respuesta de " + destino + ": " + e.getMessage(), e);
            }
        }

        throw new DescargaException(MotivoDescarga.ERROR_RED,
                "Demasiadas redirecciones (máximo " + maxRedirecciones + ") al descargar " + urlCruda);
    }

    private URI validarUrl(String urlCruda) {
        if (urlCruda == null || urlCruda.isBlank()) {
            throw new DescargaException(MotivoDescarga.URL_INVALIDA, "La URL no puede estar vacía");
        }
        String limpia = urlCruda.trim();

        URI uri;
        try {
            uri = URI.create(limpia);
        } catch (IllegalArgumentException e) {
            throw new DescargaException(MotivoDescarga.URL_INVALIDA, "La URL no es válida: " + limpia, e);
        }

        String esquema = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!esquema.equals("http") && !esquema.equals("https")) {
            throw new DescargaException(MotivoDescarga.URL_INVALIDA,
                    "La URL debe empezar por http:// o https://: " + limpia);
        }
        return uri;
    }

    private HttpResponse<InputStream> enviar(URI destino) {
        try {
            HttpRequest peticion = HttpRequest.newBuilder(destino)
                    .timeout(timeout)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/pdf,text/plain,"
                            + "application/vnd.openxmlformats-officedocument.wordprocessingml.document,*/*;q=0.8")
                    .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
                    .GET()
                    .build();
            return http.send(peticion, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException e) {
            throw new DescargaException(MotivoDescarga.ERROR_RED,
                    "No se pudo descargar " + destino + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DescargaException(MotivoDescarga.ERROR_RED, "Descarga interrumpida: " + destino, e);
        }
    }

    private static void cerrar(HttpResponse<InputStream> respuesta) {
        try {
            respuesta.body().close();
        } catch (IOException ignored) {
            // Cerrar la respuesta descartada no debe enmascarar el error original.
        }
    }

    /**
     * Algunos orígenes rechazan las descargas automatizadas (403/401/429). Sin
     * esta pista el usuario ve un HTTP 403 y no sabe que puede subir el archivo
     * a mano. Comprobado contra w3.org, que bloquea clientes que se declaran
     * navegador pero no lo son.
     */
    private static String pistaBloqueo(int status) {
        return switch (status) {
            case 401, 403, 429 -> ". El servidor de origen rechaza las descargas automatizadas:"
                    + " prueba con otra URL o descarga el archivo y súbelo a mano";
            default -> "";
        };
    }

    private byte[] leerConLimite(InputStream cuerpo, URI destino) throws IOException {
        byte[] buffer = new byte[8192];
        ByteArrayOutputStream acumulado = new ByteArrayOutputStream();
        int leidos;
        while ((leidos = cuerpo.read(buffer)) != -1) {
            if (acumulado.size() + leidos > maxBytes) {
                throw new DescargaException(MotivoDescarga.DEMASIADO_GRANDE,
                        "El archivo de " + destino + " supera el máximo permitido ("
                                + (maxBytes / (1024 * 1024)) + " MB)");
            }
            acumulado.write(buffer, 0, leidos);
        }
        return acumulado.toByteArray();
    }

    private String nombreArchivo(HttpResponse<InputStream> respuesta, URI destino) {
        String delContentDisposition = respuesta.headers().firstValue("Content-Disposition")
                .map(HttpDescargadorUrl::nombreDeContentDisposition)
                .orElse(null);
        if (delContentDisposition != null) {
            return delContentDisposition;
        }

        String ruta = destino.getPath() == null ? "" : destino.getPath();
        String ultimo = ruta.substring(ruta.lastIndexOf('/') + 1);
        String nombre = URLDecoder.decode(ultimo, StandardCharsets.UTF_8);
        if (nombre.isBlank()) {
            throw new DescargaException(MotivoDescarga.TIPO_NO_SOPORTADO,
                    "No se pudo determinar el nombre ni el tipo del archivo en " + destino
                            + ". Soportados: PDF, TXT, DOCX");
        }
        return basename(nombre);
    }

    private static String nombreDeContentDisposition(String cabecera) {
        for (String parte : cabecera.split(";")) {
            String trozo = parte.trim();
            if (trozo.toLowerCase(Locale.ROOT).startsWith("filename=")) {
                String valor = trozo.substring("filename=".length()).trim().replace("\"", "");
                String seguro = basename(valor);
                if (!seguro.isBlank() && !seguro.equals("unnamed")) {
                    return seguro;
                }
            }
        }
        return null;
    }

    /** Descarta cualquier componente de ruta que venga del servidor. */
    private static String basename(String valor) {
        if (valor == null || valor.isBlank()) {
            return "unnamed";
        }
        try {
            Path nombre = Path.of(valor).getFileName();
            if (nombre == null) {
                return "unnamed";
            }
            String texto = nombre.toString();
            return texto.equals(".") || texto.equals("..") ? "unnamed" : texto;
        } catch (InvalidPathException e) {
            return "unnamed";
        }
    }

    private static Documento.TipoDocumento deducirTipo(String contentType, String nombre, URI destino) {
        String tipo = contentType == null ? "" : contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
        Documento.TipoDocumento porCabecera = TIPOS_POR_CONTENT_TYPE.get(tipo);
        if (porCabecera != null) {
            return porCabecera;
        }

        String minusculas = nombre.toLowerCase(Locale.ROOT);
        if (minusculas.endsWith(".pdf")) {
            return Documento.TipoDocumento.PDF;
        }
        if (minusculas.endsWith(".txt")) {
            return Documento.TipoDocumento.TXT;
        }
        if (minusculas.endsWith(".docx")) {
            return Documento.TipoDocumento.DOCX;
        }

        throw new DescargaException(MotivoDescarga.TIPO_NO_SOPORTADO,
                "Tipo de archivo no soportado ("
                        + (tipo.isEmpty() ? nombre : tipo + ", " + nombre) + ") en " + destino
                        + ". Soportados: PDF, TXT, DOCX");
    }
}
