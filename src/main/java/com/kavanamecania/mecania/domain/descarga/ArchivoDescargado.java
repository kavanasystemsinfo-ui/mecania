package com.kavanamecania.mecania.domain.descarga;

import com.kavanamecania.mecania.domain.model.Documento;

/**
 * Archivo descargado de una URL y ya clasificado como documento de vehículo.
 *
 * @param nombreArchivo nombre con el que se guardará (sin componentes de ruta)
 * @param tipo          tipo deducido por Content-Type o por extensión
 * @param contenido     bytes descargados
 * @param contentType   Content-Type informado por el servidor (puede ser null)
 * @param urlFinal      URL realmente descargada (tras redirecciones)
 */
public record ArchivoDescargado(String nombreArchivo,
                                Documento.TipoDocumento tipo,
                                byte[] contenido,
                                String contentType,
                                String urlFinal) {
}
