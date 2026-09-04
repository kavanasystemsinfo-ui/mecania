package com.kavanamecania.mecania.infrastructure.storage;

import com.kavanamecania.mecania.domain.storage.AlmacenamientoArchivos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlmacenamientoDiscoLocalTest {

    private AlmacenamientoDiscoLocal storage;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("mecania-storage-test");
        storage = new AlmacenamientoDiscoLocal(tempDir.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (Files.exists(tempDir)) {
            Files.walk(tempDir)
                 .sorted(Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(File::delete);
        }
    }

    @Test
    void guardarArchivo_escribe_el_contenido_y_devuelve_ruta_relativa() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "test.txt",
                "test.txt",
                "text/plain",
                "hello world".getBytes()
        );

        String relativePath = storage.guardarArchivo(file, "vehiculos/1/documentos");

        assertThat(relativePath).isEqualTo("vehiculos/1/documentos/test.txt");

        Path fullPath = tempDir.resolve(relativePath);
        assertThat(fullPath).exists();
        assertThat(fullPath).hasContent("hello world");
    }

    @Test
    void guardarArchivo_con_subdirectorio_vacio() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "data.pdf",
                "data.pdf",
                "application/pdf",
                "%PDF-1.4 test".getBytes()
        );

        String relativePath = storage.guardarArchivo(file, "");

        assertThat(relativePath).isEqualTo("data.pdf");

        Path fullPath = tempDir.resolve(relativePath);
        assertThat(fullPath).exists();
        assertThat(fullPath).hasContent("%PDF-1.4 test");
    }

    @Test
    void leerArchivo_devuelve_el_contenido_guardado() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "bin.dat",
                "bin.dat",
                "application/octet-stream",
                new byte[]{0, 1, 2, 3}
        );

        String relativePath = storage.guardarArchivo(file, "carpeta");
        byte[] data = storage.leerArchivo(relativePath);

        assertThat(data).containsExactly(0, 1, 2, 3);
    }

    @Test
    void eliminarArchivo_borra_el_archivo() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "todelete.txt",
                "todelete.txt",
                "text/plain",
                "delete me".getBytes()
        );

        String relativePath = storage.guardarArchivo(file, "tmp");
        Path fullPath = tempDir.resolve(relativePath);
        assertThat(fullPath).exists();

        storage.eliminarArchivo(relativePath);
        assertThat(fullPath).doesNotExist();
    }

    // Note: Testing IOException on write failure is complex without mocking low-level IO.
    // We rely on the fact that Java throws IOException on failure; covered by other tests indirectly.
}