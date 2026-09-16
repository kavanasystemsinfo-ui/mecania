package com.kavanamecania.mecania.infrastructure.storage;

import com.kavanamecania.mecania.domain.storage.AlmacenamientoArchivos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
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
        // A diferencia de resolver(), aquí el propio baseDir es válido como
        // destino (subdirectorio vacío = guardar en la raíz).
        Path targetDir = baseDir.resolve(subdirectorio).normalize();
        if (!targetDir.startsWith(baseDir)) {
            throw new IOException("Path traversal detectado en subdirectorio: " + subdirectorio);
        }
        Files.createDirectories(targetDir);

        // Nunca usar el nombre original tal cual: extrae solo el nombre base y
        // descarta cualquier componente de ruta (../, subcarpetas, etc.).
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "unnamed";
        }
        String safeName = extraerNombreSeguro(originalFilename);

        // "." y ".." colapsan sobre targetDir o su padre: el nombre debe aportar
        // exactamente un elemento real debajo de targetDir.
        Path targetFile = targetDir.resolve(safeName).normalize();
        if (!targetDir.equals(targetFile.getParent()) || !targetFile.startsWith(baseDir)) {
            throw new IOException("Nombre de archivo inválido o path traversal: " + originalFilename);
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
     *
     * <p>La comprobación es léxica ({@code normalize()} + {@code startsWith()}):
     * no detecta symlinks plantados dentro del {@code baseDir}. Las rutas siempre
     * provienen del propio {@link #guardarArchivo}, nunca del usuario externo.</p>
     */
    private Path resolver(String rutaRelativa) throws IOException {
        Path resolved = baseDir.resolve(rutaRelativa).normalize();
        // "" y "." colapsan sobre el propio baseDir: la raíz nunca es un objetivo válido.
        if (resolved.equals(baseDir) || !resolved.startsWith(baseDir)) {
            throw new IOException("Ruta inválida o path traversal detectado: " + rutaRelativa);
        }
        return resolved;
    }

    /**
     * Extrae el nombre base de un nombre de archivo multipart, devolviendo
     * {@code "unnamed"} para cualquier entrada que no aporte un elemento real:
     * solo-raíz ({@code "/"}), nombres especiales ({@code "."} / {@code ".."})
     * o rutas con caracteres ilegales (byte NUL).
     */
    private String extraerNombreSeguro(String originalFilename) {
        Path namePath;
        try {
            namePath = Path.of(originalFilename).getFileName();
        } catch (InvalidPathException e) {
            return "unnamed";
        }
        if (namePath == null || namePath.toString().equals(".") || namePath.toString().equals("..")) {
            return "unnamed";
        }
        return namePath.toString();
    }
}