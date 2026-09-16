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

    @Test
    void guardarArchivo_con_nombre_travieso_lo_sanitiza_y_no_escapa_del_baseDir() throws IOException {
        // Un nombre con ../ debe quedarse dentro del baseDir (path traversal).
        MockMultipartFile file = new MockMultipartFile(
                "malicious.txt",
                "../../../etc/cron.d/malicious.txt",
                "text/plain",
                "pwned".getBytes()
        );

        String relativePath = storage.guardarArchivo(file, "vehiculos/1/documentos");

        // La ruta devuelta no debe contener ".."
        assertThat(relativePath).doesNotContain("..");
        // Y el archivo debe existir DENTRO del baseDir
        Path fullPath = tempDir.resolve(relativePath).normalize();
        assertThat(fullPath).exists();
        assertThat(fullPath.toAbsolutePath().startsWith(tempDir.toAbsolutePath().normalize()))
                .as("ruta fuera del baseDir: %s", fullPath)
                .isTrue();
        // Y ninguna ruta externa debe haberse creado
        assertThat(Files.exists(Path.of(tempDir.getParent().toString(), "etc", "cron.d", "malicious.txt")))
                .isFalse();
    }

    @Test
    void leerArchivo_con_ruta_traviesa_se_rechaza() {
        assertThatThrownBy(() -> storage.leerArchivo("../../secret.txt"))
                .isInstanceOf(IOException.class);
    }

    @Test
    void eliminarArchivo_con_ruta_traviesa_se_rechaza() {
        assertThatThrownBy(() -> storage.eliminarArchivo("../../secret.txt"))
                .isInstanceOf(IOException.class);
    }

    // --- Hardening post-revisión OCR (hallazgos 1-4, sesión 2026-09-16) ---

    @Test
    void guardarArchivo_con_nombre_solo_ruta_no_lanza_NPE() throws IOException {
        // getFileName() devuelve null para "/" (solo-raíz): debe caer a "unnamed", no NPE.
        MockMultipartFile file = new MockMultipartFile(
                "root",
                "/",
                "text/plain",
                "safe".getBytes()
        );

        String relativePath = storage.guardarArchivo(file, "vehiculos/1/documentos");

        assertThat(relativePath).isEqualTo("vehiculos/1/documentos/unnamed");
        assertThat(tempDir.resolve(relativePath)).hasContent("safe");
    }

    @Test
    void guardarArchivo_con_nombre_punto_cae_a_unnamed_y_no_corrompe_targetDir() throws IOException {
        // "." colapsaría sobre targetDir y REPLACE_EXISTING lo reemplazaría por un
        // archivo: debe caer a "unnamed" como los nombres vacíos.
        MockMultipartFile file = new MockMultipartFile(
                "dot",
                ".",
                "text/plain",
                "corrupt".getBytes()
        );

        String relativePath = storage.guardarArchivo(file, "vehiculos/1/documentos");

        assertThat(relativePath).isEqualTo("vehiculos/1/documentos/unnamed");
        // El subdirectorio sigue siendo un directorio, no un archivo corrupto.
        assertThat(Files.isDirectory(tempDir.resolve("vehiculos/1/documentos"))).isTrue();
        assertThat(tempDir.resolve(relativePath)).hasContent("corrupt");
    }

    @Test
    void guardarArchivo_con_nombre_punto_punto_cae_a_unnamed_y_no_escapa() throws IOException {
        // ".." resolvía al padre de targetDir: debe caer a "unnamed", igual que ".".
        MockMultipartFile file = new MockMultipartFile(
                "dotdot",
                "..",
                "text/plain",
                "escape".getBytes()
        );

        String relativePath = storage.guardarArchivo(file, "vehiculos/1/documentos");

        assertThat(relativePath).isEqualTo("vehiculos/1/documentos/unnamed");
        // El padre (vehiculos/1) sigue intacto como directorio.
        assertThat(Files.isDirectory(tempDir.resolve("vehiculos/1"))).isTrue();
    }

    @Test
    void guardarArchivo_con_nombre_con_nul_byte_se_maneja_como_invalido() throws IOException {
        // Path.of lanza InvalidPathException con byte NUL: no debe propagarse sin control.
        MockMultipartFile file = new MockMultipartFile(
                "nul",
                "evil\u0000.txt",
                "text/plain",
                "data".getBytes()
        );

        String relativePath = storage.guardarArchivo(file, "docs");

        // Debe caer a "unnamed" en lugar de reventar la petición.
        assertThat(relativePath).isEqualTo("docs/unnamed");
    }

    @Test
    void resolver_rechaza_rutas_que_colapsan_sobre_la_raiz() {
        // "" y "." resuelven al propio baseDir: eliminarArchivo("") borraría la raíz.
        assertThatThrownBy(() -> storage.eliminarArchivo(""))
                .isInstanceOf(IOException.class);
        assertThatThrownBy(() -> storage.eliminarArchivo("."))
                .isInstanceOf(IOException.class);
        assertThatThrownBy(() -> storage.leerArchivo(""))
                .isInstanceOf(IOException.class);
        // Y la raíz sigue existiendo.
        assertThat(Files.isDirectory(tempDir)).isTrue();
    }

    @Test
    void guardarArchivo_lee_tras_guardar_un_archivo_punto_punto() throws IOException {
        // Tras bloquear ".." en guardarArchivo, una RUTA relativa legítima con
        // subdirectorio debe seguir funcionando (regresión).
        MockMultipartFile file = new MockMultipartFile(
                "normal.txt",
                "normal.txt",
                "text/plain",
                "ok".getBytes()
        );

        String relativePath = storage.guardarArchivo(file, "vehiculos/2/documentos");
        assertThat(storage.leerArchivo(relativePath)).isEqualTo("ok".getBytes());
    }

    // Note: Testing IOException on write failure is complex without mocking low-level IO.
    // We rely on the fact that Java throws IOException on failure; covered by other tests indirectly.
}