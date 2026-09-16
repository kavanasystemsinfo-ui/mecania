package com.kavanamecania.mecania.domain.vector;

import com.kavanamecania.mecania.domain.embedding.Embedding;

import java.util.List;

/**
 * Almacén de vectores de los fragmentos. Vive en domain porque "guardar un
 * vector y recuperar los más parecidos" es qué queremos; la implementación
 * (pgvector vía JDBC nativo) es infraestructura.
 *
 * <p>La búsqueda SIEMPRE queda restringida a un vehículo: ningún método de
 * esta interfaz permite cruzar manuales de vehículos distintos.</p>
 */
public interface RepositorioVectores {

    /**
     * Persiste el vector de un fragmento ya guardado.
     *
     * @param fragmentoId id del fragmento (debe existir en la tabla fragmentos)
     * @param embedding   vector a guardar
     * @throws VectorPersistenceException si el almacén no está disponible
     */
    void guardar(Long fragmentoId, Embedding embedding) throws VectorPersistenceException;

    /**
     * Devuelve los fragmentos más parecidos a la consulta, restringidos al
     * vehículo indicado, ordenados de mayor a menor similitud.
     *
     * @param vehiculoId vehículo al que pertenecen los manuales (filtro obligatorio)
     * @param consulta   embedding de la pregunta
     * @param topK       máximo de fragmentos a devolver
     */
    List<FragmentoSimilar> buscarSimilares(Long vehiculoId, Embedding consulta, int topK)
            throws VectorPersistenceException;
}
