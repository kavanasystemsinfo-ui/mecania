package com.kavanamecania.mecania.domain.busqueda;

/**
 * La búsqueda externa no se pudo completar (bloqueo anti-bot, error HTTP o
 * caída de red). Se distingue de "no hay resultados": el usuario debe saber
 * que la búsqueda falló y que puede importar la URL a mano.
 */
public class BusquedaException extends RuntimeException {

    public BusquedaException(String mensaje) {
        super(mensaje);
    }

    public BusquedaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
