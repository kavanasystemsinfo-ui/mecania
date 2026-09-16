package com.kavanamecania.mecania.domain.descarga;

/**
 * La descarga de un manual remoto no se pudo completar. Lleva el motivo para
 * que la capa web responda con el código adecuado (400, 413, 415 o 502).
 */
public class DescargaException extends RuntimeException {

    private final MotivoDescarga motivo;

    public DescargaException(MotivoDescarga motivo, String mensaje) {
        super(mensaje);
        this.motivo = motivo;
    }

    public DescargaException(MotivoDescarga motivo, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.motivo = motivo;
    }

    public MotivoDescarga motivo() {
        return motivo;
    }
}
