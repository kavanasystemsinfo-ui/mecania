package com.kavanamecania.mecania.domain.model;

/**
 * Cómo se repite una alerta.
 */
public enum Repetitividad {
    /** Se avisa una sola vez y queda desactivada. */
    UNICA,
    /** Se avisa cada mes mientras esté activa. */
    MENSUAL,
    /** Se avisa cada año mientras esté activa. */
    ANUAL
}
