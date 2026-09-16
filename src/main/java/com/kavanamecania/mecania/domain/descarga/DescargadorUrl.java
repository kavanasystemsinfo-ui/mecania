package com.kavanamecania.mecania.domain.descarga;

/**
 * Descarga el contenido de una URL indicada por el usuario.
 *
 * <p>La elección de la URL es del usuario, así que la implementación debe
 * validar el destino (esquema, host y cada salto de redirección), limitar el
 * tamaño y rechazar lo que no sea un documento soportado.</p>
 */
public interface DescargadorUrl {

    /**
     * @param url URL http/https indicada por el usuario
     * @return el archivo descargado, con nombre y tipo ya resueltos
     * @throws DescargaException con el motivo concreto del rechazo o del fallo
     */
    ArchivoDescargado descargar(String url);
}
