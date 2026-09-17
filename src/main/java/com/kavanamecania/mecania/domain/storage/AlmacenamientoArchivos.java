package com.kavanamecania.mecania.domain.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Estrategia para almacenar archivos de manuales.
 * La implementación debe ser thread-safe.
 */
public interface AlmacenamientoArchivos {
    /**
     * Guarda el archivo recibido y devuelve la ruta relativa al directorio base de almacenamiento.
     *
     * @param file archivo multipart a guardar
     * @param subdirectorio subcarpeta dentro del storage (ej. "vehiculos/123/documentos")
     * @return ruta relativa (ej. "vehiculos/123/documentos/manual.pdf") que se guardará en BD
     * @throws IOException si falla la escritura
     */
    String guardarArchivo(MultipartFile file, String subdirectorio) throws IOException;

    /**
     * Guarda un contenido ya en memoria (por ejemplo un manual descargado de
     * internet) y devuelve la ruta relativa al directorio base.
     *
     * @param contenido   bytes a escribir (no puede estar vacío)
     * @param nombreArchivo nombre propuesto; se sanitiza igual que en la subida
     * @param subdirectorio subcarpeta dentro del storage
     * @return ruta relativa que se guardará en BD
     * @throws IOException si falla la escritura o la ruta no es válida
     */
    String guardarArchivo(byte[] contenido, String nombreArchivo, String subdirectorio) throws IOException;

    /**
     * Lee el archivo completo y devuelve su contenido como array de bytes.
     *
     * @param rutaRelativa ruta tal como se obtuvo de {@link #guardarArchivo}
     * @return contenido del archivo
     * @throws IOException si no se puede leer
     */
    byte[] leerArchivo(String rutaRelativa) throws IOException;

    /**
     * Elimina el archivo asociado a la ruta relativa.
     *
     * @param rutaRelativa ruta tal como se obtuvo de {@link #guardarArchivo}
     * @throws IOException si falla la eliminación
     */
    void eliminarArchivo(String rutaRelativa) throws IOException;

    /**
     * Lee el archivo como flujo, sin cargarlo entero en memoria. La
     * implementación por defecto reutiliza {@link #leerArchivo}; los almacenes de
     * objetos lo sobrescriben para no traerse el fichero completo a heap.
     *
     * @param rutaRelativa ruta tal como se obtuvo de {@link #guardarArchivo}
     * @return flujo de lectura; quien lo abre es quien lo cierra
     * @throws IOException si no se puede leer
     */
    default InputStream leerArchivoStream(String rutaRelativa) throws IOException {
        return new ByteArrayInputStream(leerArchivo(rutaRelativa));
    }

    /**
     * Comprueba que el almacén está operativo (directorio escribible, bucket que
     * responde). Lo usa {@code /health/ready}: un servicio que dice estar listo
     * con el almacenamiento caído miente.
     */
    default boolean disponible() {
        return true;
    }
}