package com.kavanamecania.mecania.domain.descarga;

/**
 * Motivo por el que una descarga se rechazó, para poder responder con un
 * código HTTP distinto y un mensaje honesto en cada caso.
 */
public enum MotivoDescarga {
    /** La URL está vacía, mal formada o no usa http/https. */
    URL_INVALIDA,
    /** El host es local, privado o de metadatos (protección SSRF). */
    HOST_NO_PERMITIDO,
    /** Se descargó algo, pero no es un tipo de documento soportado. */
    TIPO_NO_SOPORTADO,
    /** El archivo supera el tamaño máximo permitido. */
    DEMASIADO_GRANDE,
    /** El servidor de origen falló, redirigió en exceso o no respondió. */
    ERROR_RED
}
