package com.kavanamecania.mecania.application;

import com.kavanamecania.mecania.domain.model.Documento;

/**
 * Resultado de importar un manual desde una URL.
 *
 * @param documento el documento registrado en el vehículo
 * @param yaExistia true si esa URL ya estaba importada para este vehículo (no
 *                  se descargó de nuevo ni se reprocesó)
 */
public record ResultadoImportacion(Documento documento, boolean yaExistia) {
}
