package com.kavanamecania.mecania.infrastructure.storage;

import com.kavanamecania.mecania.domain.storage.AlmacenamientoArchivos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contrato común de las implementaciones de {@link AlmacenamientoArchivos}.
 *
 * <p>Disco y almacén de objetos tienen reglas propias en los detalles (rutas vs.
 * claves), pero NO pueden discrepar en lo que protege al sistema: el mismo
 * nombre tiene que producir la misma ruta relativa y los mismos intentos de
 * salirse del almacén tienen que fallar en los dos sitios. Si alguien cambia la
 * sanitización de uno solo, este test lo caza.</p>
 */
@ExtendWith(MockitoExtension.class)
class AlmacenamientoContratoTest {

    @Mock private S3Client s3;

    private AlmacenamientoArchivos discoLocal(Path base) {
        return new AlmacenamientoDiscoLocal(base.toString());
    }

    private AlmacenamientoArchivos objetos() {
        return new AlmacenamientoS3("mecania-manuales", s3);
    }

    @Test
    void los_dos_backends_dan_la_misma_ruta_para_el_mismo_nombre() throws IOException {
        Path base = Files.createTempDirectory("contrato-mecania");
        AlmacenamientoArchivos disco = discoLocal(base);
        AlmacenamientoArchivos s3Almacen = objetos();

        for (String nombre : new String[]{"manual.pdf", "../../etc/passwd", "/absoluto/manual.txt", ".", "..", "carpeta/manual.txt"}) {
            MockMultipartFile archivo = new MockMultipartFile("file", nombre, "application/pdf",
                    "contenido".getBytes(StandardCharsets.UTF_8));
            String rutaDisco = disco.guardarArchivo(archivo, "vehiculos/1/documentos");
            String claveS3 = s3Almacen.guardarArchivo(archivo, "vehiculos/1/documentos");

            assertThat(claveS3)
                    .as("misma ruta relativa para el nombre %s", nombre)
                    .isEqualTo(rutaDisco);
            assertThat(claveS3).startsWith("vehiculos/1/documentos/");
            assertThat(claveS3).doesNotContain("..");
        }
    }

    @Test
    void los_dos_backends_rechazan_un_subdirectorio_que_escapa() throws IOException {
        Path base = Files.createTempDirectory("contrato-mecania-escape");
        MockMultipartFile archivo = new MockMultipartFile("file", "manual.pdf", "application/pdf",
                "x".getBytes(StandardCharsets.UTF_8));

        for (AlmacenamientoArchivos almacen : new AlmacenamientoArchivos[]{discoLocal(base), objetos()}) {
            assertThatThrownBy(() -> almacen.guardarArchivo(archivo, "../fuera"))
                    .as("backend %s", almacen.getClass().getSimpleName())
                    .isInstanceOf(IOException.class);
        }
    }

    @Test
    void los_dos_backends_rechazan_contenido_vacio() throws IOException {
        Path base = Files.createTempDirectory("contrato-mecania-vacio");

        for (AlmacenamientoArchivos almacen : new AlmacenamientoArchivos[]{discoLocal(base), objetos()}) {
            assertThatThrownBy(() -> almacen.guardarArchivo(new byte[0], "vacio.pdf", "vehiculos/1/documentos"))
                    .as("backend %s", almacen.getClass().getSimpleName())
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
