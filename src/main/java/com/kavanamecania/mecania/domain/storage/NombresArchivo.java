package com.kavanamecania.mecania.domain.storage;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Reglas de nombres y subdirectorios comunes a las implementaciones de
 * {@link AlmacenamientoArchivos}.
 *
 * <p>Existen para que la sanitización no dependa del backend: en disco el
 * peligro es {@code ../} y en un bucket de objetos es una clave con prefijos
 * inesperados, pero las dos cosas se evitan con la misma regla. Cualquier cambio
 * aquí afecta a los dos sitios, y por eso las dos implementaciones se someten al
 * mismo test de contrato ({@code AlmacenamientoContratoTest}).</p>
 */
public final class NombresArchivo {

    private static final Pattern S3_BUCKET = Pattern.compile("^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$");

    private NombresArchivo() {
    }

    /**
     * Extrae el nombre base de un nombre de archivo, devolviendo {@code "unnamed"}
     * para cualquier entrada que no aporte un elemento real: solo-ruta ({@code "/"}),
     * nombres especiales ({@code "."} / {@code ".."}) o rutas con caracteres
     * ilegales (byte NUL). Mismas reglas que aplica el almacenamiento en disco.
     */
    public static String seguro(String nombreOriginal) {
        if (nombreOriginal == null || nombreOriginal.isBlank()) {
            return "unnamed";
        }
        Path namePath;
        try {
            namePath = Path.of(nombreOriginal).getFileName();
        } catch (InvalidPathException e) {
            return "unnamed";
        }
        if (namePath == null
                || namePath.toString().equals(".")
                || namePath.toString().equals("..")) {
            return "unnamed";
        }
        return namePath.toString();
    }

    /**
     * Normaliza el subdirectorio a un prefijo de clave seguro (sin barra inicial,
     * sin segmentos vacíos, sin {@code .} ni {@code ..} y sin letra de unidad de
     * Windows). El subdirectorio vacío es válido y significa "en la raíz".
     */
    public static String subdirectorioSeguro(String subdirectorio) throws IOException {
        if (subdirectorio == null || subdirectorio.isBlank()) {
            return "";
        }
        String normalizado = subdirectorio.trim().replace('\\', '/');
        if (normalizado.startsWith("/") || normalizado.matches("^[A-Za-z]:.*")) {
            throw new IOException("Subdirectorio inválido (ruta absoluta): " + subdirectorio);
        }
        String limpio = normalizado.replaceAll("/{2,}", "/");
        for (String segmento : limpio.split("/")) {
            if (segmento.isEmpty() || segmento.equals(".") || segmento.equals("..")) {
                throw new IOException("Path traversal detectado en subdirectorio: " + subdirectorio);
            }
        }
        return limpio;
    }

    /**
     * Clave final dentro del almacén: {@code subdirectorio/nombre} o solo el nombre
     * si el subdirectorio está vacío. Es el valor que se guarda en base de datos.
     */
    public static String clave(String nombreOriginal, String subdirectorio) throws IOException {
        String prefijo = subdirectorioSeguro(subdirectorio);
        String nombre = seguro(nombreOriginal);
        return prefijo.isEmpty() ? nombre : prefijo + "/" + nombre;
    }

    /** Valida el nombre de un bucket de S3 antes de intentar ninguna llamada. */
    public static void validarBucket(String bucket) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("mecania.storage.s3.bucket no está configurado");
        }
        if (!S3_BUCKET.matcher(bucket).matches()) {
            throw new IllegalStateException("mecania.storage.s3.bucket no es un nombre de bucket válido: " + bucket);
        }
    }
}
