package com.kavanamecania.mecania.infrastructure.storage;

import com.kavanamecania.mecania.domain.storage.AlmacenamientoArchivos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Implementación de AlmacenamientoArchivos que guarda los archivos en el sistema de archivos local.
 * El directorio base es configurable mediante la propiedad mecania.storage.dir.
 *
 * <p>Seguridad: rutas relativas y nombres de archivo se sanitizan contra path
 * traversal ({@code ..}). Toda operación comprueba que la ruta resuelta quede
 * dentro del {@code baseDir}.</p>
 */
@Component
public class AlmacenamientoDiscoLocal implements AlmacenamientoArchivos {

    private final Path baseDir;

    public AlmacenamientoDiscoLocal(@Value("${mecania.storage.dir:${user.home}/mecania-storage}") String baseDir) {
        this.baseDir = Path.of(baseDir).toAbsolutePath().normalize();
        // Ensure the base directory exists
        try {
            Files.createDirectories(this.baseDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create storage directory: " + baseDir, e);
        }
    }

    @Override
    public String guardarArchivo(MultipartFile file, String subdirectorio) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Sanitizamos el subdirectorio y resolvemos contra baseDir, comprobando
        // que no escape (defensa en profundidad: el caller usa ids numéricos).
        Path targetDir = resolver(subdirectorio);
        Files.createDirectories(targetDir);

        // Nunca usar el nombre original tal cual: extrae solo el nombre base y
        // descarta cualquier componente de ruta (../, subcarpetas, etc.).
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "unnamed";
        }
        String safeName = Path.of(originalFilename).getFileName().toString();

        Path targetFile = targetDir.resolve(safeName).normalize();
        if (!targetFile.startsWith(baseDir)) {
            throw new IOException("Path traversal detectado en nombre de archivo: " + originalFilename);
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }

        // Return relative path from baseDir
        return baseDir.relativize(targetFile).toString();
    }

    @Override
    public byte[] leerArchivo(String rutaRelativa) throws IOException {
        Path fullPath = resolver(rutaRelativa);
        if (!Files.exists(fullPath)) {
            throw new IOException("File not found: " + rutaRelativa);
        }
        return Files.readAllBytes(fullPath);
    }

    @Override
    public void eliminarArchivo(String rutaRelativa) throws IOException {
        Path fullPath = resolver(rutaRelativa);
        if (!Files.exists(fullPath)) {
            // Already deleted? Consider idempotent.
            return;
        }
        Files.delete(fullPath);
    }

    /**
     * Resuelve una ruta relativa contra el baseDir garantizando que el resultado
     * no escape de él (protección contra path traversal con {@code ..}).
     */
    private Path resolver(String rutaRelativa) throws IOException {
        Path resolved = baseDir.resolve(rutaRelativa).normalize();
        if (!resolved.startsWith(baseDir)) {
            throw new IOException("Path traversal detectado en ruta: " + rutaRelativa);
        }
        return resolved;
    }
}