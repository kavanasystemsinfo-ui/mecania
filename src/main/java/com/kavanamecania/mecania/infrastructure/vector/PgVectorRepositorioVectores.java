package com.kavanamecania.mecania.infrastructure.vector;

import com.kavanamecania.mecania.domain.embedding.Embedding;
import com.kavanamecania.mecania.domain.vector.FragmentoSimilar;
import com.kavanamecania.mecania.domain.vector.RepositorioVectores;
import com.kavanamecania.mecania.domain.vector.VectorPersistenceException;
import org.postgresql.util.PGobject;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;

/**
 * Implementación de {@link RepositorioVectores} sobre pgvector vía JDBC nativo.
 *
 * <p>Hibernate-core no sabe mapear el tipo {@code vector}, así que este repositorio
 * usa {@link JdbcTemplate} y {@link PGobject} con tipo "vector" para insertar y
 * comparar por distancia coseno (operador {@code <=>}). La tabla
 * {@code fragmento_embeddings} la crea {@link PgVectorSchemaInitializer}; nunca
 * la toca Hibernate (por eso puede convivir con ddl-auto=update).</p>
 *
 * <p>El {@link JdbcTemplate} participa de la transacción JPA (Spring Boot liga el
 * DataSource al JpaTransactionManager), de modo que si el procesamiento revierte,
 * el vector insertado también revierte.</p>
 */
@Component
public class PgVectorRepositorioVectores implements RepositorioVectores {

    private final JdbcTemplate jdbcTemplate;

    public PgVectorRepositorioVectores(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void guardar(Long fragmentoId, Embedding embedding) throws VectorPersistenceException {
        try {
            jdbcTemplate.update(
                    "INSERT INTO fragmento_embeddings (fragmento_id, embedding) VALUES (?, ?)",
                    fragmentoId,
                    toPgVector(embedding));
        } catch (DataAccessException e) {
            throw new VectorPersistenceException(
                    "No se pudo guardar el vector del fragmento " + fragmentoId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<FragmentoSimilar> buscarSimilares(Long vehiculoId, Embedding consulta, int topK)
            throws VectorPersistenceException {
        try {
            // La restricción de vehículo es el WHERE d.vehiculo_id: imposible cruzar manuales.
            // distancia = coseno; devolvemos similitud = 1 - distancia para leerla de forma natural.
            return jdbcTemplate.query("""
                            SELECT f.id, f.documento_id, f.posicion, f.texto, (e.embedding <=> ?) AS distancia
                            FROM fragmento_embeddings e
                            JOIN fragmentos f ON f.id = e.fragmento_id
                            JOIN documentos d ON d.id = f.documento_id
                            WHERE d.vehiculo_id = ?
                            ORDER BY distancia ASC
                            LIMIT ?
                            """,
                    (rs, rowNum) -> new FragmentoSimilar(
                            rs.getLong("id"),
                            rs.getLong("documento_id"),
                            rs.getInt("posicion"),
                            rs.getString("texto"),
                            1.0 - rs.getDouble("distancia")),
                    toPgVector(consulta),
                    vehiculoId,
                    topK);
        } catch (DataAccessException e) {
            throw new VectorPersistenceException(
                    "No se pudo buscar fragmentos similares: " + e.getMessage(), e);
        }
    }

    private static PGobject toPgVector(Embedding embedding) throws VectorPersistenceException {
        try {
            PGobject pg = new PGobject();
            pg.setType("vector");
            pg.setValue(embedding.toPgVectorString());
            return pg;
        } catch (SQLException e) {
            throw new VectorPersistenceException("Error serializando el vector", e);
        }
    }
}
