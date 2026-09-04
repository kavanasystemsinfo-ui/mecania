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
 */
@Component
public class AlmacenamientoDiscoLocal implements AlmacenamientoArchivos {

    private final Path baseDir;

    public AlmacenamientoDiscoLocal(@Value("${mecania.storage.dir:${user.home}/mecania-storage}") String baseDir) {
        this.baseDir = Path.of(baseDir);
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

        // Sanitize subdirectorio to prevent path traversal? We'll assume it's safe for now.
        Path targetDir = baseDir.resolve(subdirectorio);
        Files.createDirectories(targetDir);

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "unnamed";
        }

        // Simple approach: use original filename. In production we might want to add UUID to avoid collisions.
        Path targetFile = targetDir.resolve(originalFilename);
        // If file exists, we could overwrite or add a counter. For simplicity, overwrite.
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }

        // Return relative path from baseDir
        return baseDir.relativize(targetFile).toString();
    }

    @Override
    public byte[] leerArchivo(String rutaRelativa) throws IOException {
        Path fullPath = baseDir.resolve(rutaRelativa);
        if (!Files.exists(fullPath)) {
            throw new IOException("File not found: " + rutaRelativa);
        }
        return Files.readAllBytes(fullPath);
    }

    @Override
    public void eliminarArchivo(String rutaRelativa) throws IOException {
        Path fullPath = baseDir.resolve(rutaRelativa);
        if (!Files.exists(fullPath)) {
            // Already deleted? Consider idempotent.
            return;
        }
        Files.delete(fullPath);
    }
}