package com.kavanamecania.mecania.domain.busqueda;

import java.util.List;

/**
 * Busca posibles manuales del vehículo en fuentes públicas.
 *
 * <p>Es una búsqueda ASISTIDA: el buscador solo PROPONE candidatos. La descarga
 * es una decisión explícita del usuario (ver {@code DescargadorUrl}), y ninguna
 * implementación debe descargar ni almacenar nada por su cuenta.</p>
 */
public interface BuscadorManuales {

    /**
     * @param consulta texto de búsqueda ya construido (marca, modelo, año, ...)
     * @return lista de candidatos (posiblemente vacía si no hay resultados)
     * @throws BusquedaException si el buscador no está disponible o bloquea la
     *                           petición. Nunca se devuelve una lista vacía para
     *                           disimular un fallo.
     */
    List<CandidatoManual> buscar(String consulta);
}
