package com.kavanamecania.mecania.domain.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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
}