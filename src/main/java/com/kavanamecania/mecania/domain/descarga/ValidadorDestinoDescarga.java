package com.kavanamecania.mecania.domain.descarga;

import java.net.URI;

/**
 * Comprueba que un destino de descarga sea aceptable antes de abrir conexión.
 * Se aplica a la URL inicial y a cada redirección.
 *
 * <p>Se inyecta en el descargador para poder sustituirlo en pruebas y en
 * entornos locales sin relajar la comprobación en producción.</p>
 */
@FunctionalInterface
public interface ValidadorDestinoDescarga {

    void validar(URI destino);

    /**
     * Validador sin restricciones de host. Solo para desarrollo local y tests
     * contra un servidor propio: en producción un destino privado es la vía
     * clásica para leer metadatos del servidor (SSRF).
     */
    static ValidadorDestinoDescarga permitirTodo() {
        return destino -> { };
    }
}
