package com.kavanamecania.mecania.infrastructure.vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Crea el esquema de pgvector al arrancar, SOLO sobre PostgreSQL.
 *
 * <p>Corre como {@link ApplicationRunner}: así se ejecuta después de que Hibernate
 * (ddl-auto=update) haya creado la tabla {@code fragmentos}, que es la que
 * referencia la tabla auxiliar. Sobre H2 (tests) no hace nada, porque H2 no
 * tiene pgvector.</p>
 *
 * <p>La extensión y la tabla se crean con IF NOT EXISTS: idempotente entre
 * arranques. El tipo es {@code vector} sin dimensión fija para no acoplar el
 * esquema al modelo de embeddings; pgvector solo exige que consulta y datos
 * tengan la misma dimensión al comparar.</p>
 */
@Component
public class PgVectorSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PgVectorSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public PgVectorSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        String producto = jdbcTemplate.execute((ConnectionCallback<String>) connection ->
                connection.getMetaData().getDatabaseProductName());
        if (producto == null || !producto.equalsIgnoreCase("PostgreSQL")) {
            return; // H2 en tests: no hay pgvector, el repositorio se mockea.
        }

        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS fragmento_embeddings (
                    fragmento_id BIGINT PRIMARY KEY REFERENCES fragmentos(id) ON DELETE CASCADE,
                    embedding vector NOT NULL
                )
                """);
        log.info("pgvector listo: extensión y tabla fragmento_embeddings disponibles");
    }
}
