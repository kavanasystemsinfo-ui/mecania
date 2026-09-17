package com.kavanamecania.mecania.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Comprobación honesta del estado del servicio.
 *
 * <p>Existe por una razón concreta: en el despliegue del 2026-09-17 el
 * endpoint de salud devolvía 200 mientras la clave de embeddings no estaba
 * configurada y el pipeline entero de IA estaba muerto. Un health check que
 * solo mira la base de datos miente. Aquí se comprueban las tres dependencias
 * de las que depende que el producto haga lo que promete.</p>
 */
@Component
public class DiagnosticoEntorno {

    private static final Logger log = LoggerFactory.getLogger(DiagnosticoEntorno.class);

    private final JdbcTemplate jdbcTemplate;
    private final String claveEmbeddingsConfigurada;
    private final Path directorioAlmacenamiento;

    public DiagnosticoEntorno(
            JdbcTemplate jdbcTemplate,
            @Value("${mecania.embedding.api-key:}") String claveEmbeddingsConfigurada,
            @Value("${mecania.storage.dir:${user.home}/mecania-storage}") String directorioAlmacenamiento) {
        this.jdbcTemplate = jdbcTemplate;
        this.claveEmbeddingsConfigurada = claveEmbeddingsConfigurada;
        this.directorioAlmacenamiento = Path.of(directorioAlmacenamiento).toAbsolutePath().normalize();
    }

    /** La base de datos responde. */
    public boolean baseDatosOk() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            log.warn("Comprobación de base de datos fallida: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Hay clave para calcular embeddings: la property o, si está vacía, la
     * variable de entorno (misma regla que usa el servicio de embeddings).
     */
    public boolean claveEmbeddingsPresente() {
        String clave = (claveEmbeddingsConfigurada == null || claveEmbeddingsConfigurada.isBlank())
                ? System.getenv("OPENROUTER_API_KEY")
                : claveEmbeddingsConfigurada;
        return clave != null && !clave.isBlank();
    }

    /** El directorio de almacenamiento existe y acepta escrituras. */
    public boolean almacenamientoEscribible() {
        try {
            Files.createDirectories(directorioAlmacenamiento);
            Path prueba = Files.createTempFile(directorioAlmacenamiento, ".escritura-", ".tmp");
            Files.deleteIfExists(prueba);
            return true;
        } catch (Exception e) {
            log.warn("Almacenamiento no escribible en {}: {}", directorioAlmacenamiento, e.getMessage());
            return false;
        }
    }
}
