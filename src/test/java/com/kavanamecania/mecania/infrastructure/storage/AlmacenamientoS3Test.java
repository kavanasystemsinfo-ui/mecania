package com.kavanamecania.mecania.infrastructure.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Almacenamiento en objetos compatible con S3 (Cloudflare R2).
 *
 * <p>El cliente S3 se mockea: lo que se prueba aquí es la construcción de la
 * clave, el contrato de errores y que ninguna ruta se escape del prefijo. La
 * verificación contra un servidor S3 real (MinIO local o R2) es el smoke test de
 * despliegue, porque un mock no demuestra que el bucket responda.</p>
 */
@ExtendWith(MockitoExtension.class)
class AlmacenamientoS3Test {

    private static final String BUCKET = "mecania-manuales";

    @Mock private S3Client s3;

    private AlmacenamientoS3 almacenamiento;

    @BeforeEach
    void setUp() {
        almacenamiento = new AlmacenamientoS3(BUCKET, s3);
    }

    @Test
    void guardarArchivo_subeConLaClaveDelSubdirectorioYDevuelveEsaClave() throws IOException {
        MockMultipartFile archivo = new MockMultipartFile("file", "manual.pdf", "application/pdf",
                "contenido".getBytes(StandardCharsets.UTF_8));

        String clave = almacenamiento.guardarArchivo(archivo, "vehiculos/7/documentos");

        assertThat(clave).isEqualTo("vehiculos/7/documentos/manual.pdf");
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(captor.capture(), any(RequestBody.class));
        assertThat(captor.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(captor.getValue().key()).isEqualTo("vehiculos/7/documentos/manual.pdf");
        assertThat(captor.getValue().contentType()).isEqualTo("application/pdf");
        assertThat(captor.getValue().contentLength()).isEqualTo(9L);
    }

    @Test
    void guardarArchivo_sinSubdirectorio_dejaElNombreEnLaRaiz() throws IOException {
        MockMultipartFile archivo = new MockMultipartFile("file", "manual.txt", "text/plain",
                "hola".getBytes(StandardCharsets.UTF_8));

        assertThat(almacenamiento.guardarArchivo(archivo, "")).isEqualTo("manual.txt");
        assertThat(almacenamiento.guardarArchivo(archivo, null)).isEqualTo("manual.txt");
    }

    @Test
    void guardarArchivo_conNombreTravieso_soloTomaElNombreBase() throws IOException {
        MockMultipartFile archivo = new MockMultipartFile("file", "../../../etc/passwd", "text/plain",
                "x".getBytes(StandardCharsets.UTF_8));

        String clave = almacenamiento.guardarArchivo(archivo, "vehiculos/7/documentos");

        assertThat(clave).isEqualTo("vehiculos/7/documentos/passwd");
    }

    @Test
    void guardarArchivo_conSubdirectorioTravieso_seRechazaSinLlamarAlProveedor() {
        MockMultipartFile archivo = new MockMultipartFile("file", "manual.pdf", "application/pdf",
                "x".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> almacenamiento.guardarArchivo(archivo, "vehiculos/../7"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Path traversal");
        assertThatThrownBy(() -> almacenamiento.guardarArchivo(archivo, "/absoluto"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("ruta absoluta");
        verify(s3, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void guardarArchivo_conContenidoVacio_seRechaza() {
        assertThatThrownBy(() -> almacenamiento.guardarArchivo(new byte[0], "vacio.txt", "docs"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> almacenamiento.guardarArchivo((byte[]) null, "nulo.txt", "docs"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void guardarBytes_subeElContenidoDescargadoDeInternet() throws IOException {
        byte[] contenido = "manual descargado".getBytes(StandardCharsets.UTF_8);

        String clave = almacenamiento.guardarArchivo(contenido, "manual.pdf", "vehiculos/3/documentos");

        assertThat(clave).isEqualTo("vehiculos/3/documentos/manual.pdf");
        verify(s3).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void leerArchivo_devuelveElContenidoDelObjeto() throws IOException {
        when(s3.getObject(any(GetObjectRequest.class))).thenReturn(respuestaCon("manual completo"));

        byte[] leido = almacenamiento.leerArchivo("vehiculos/3/documentos/manual.txt");

        assertThat(new String(leido, StandardCharsets.UTF_8)).isEqualTo("manual completo");
        ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3).getObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(captor.getValue().key()).isEqualTo("vehiculos/3/documentos/manual.txt");
    }

    @Test
    void leerArchivoStream_devuelveElFlujoDelProveedorSinCargarloEnMemoria() throws IOException {
        when(s3.getObject(any(GetObjectRequest.class))).thenReturn(respuestaCon("en flujo"));

        try (InputStream entrada = almacenamiento.leerArchivoStream("vehiculos/3/documentos/manual.txt")) {
            assertThat(new String(entrada.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("en flujo");
        }
    }

    @Test
    void leerArchivo_objetoInexistente_daErrorClaro() {
        when(s3.getObject(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("no existe").build());

        assertThatThrownBy(() -> almacenamiento.leerArchivo("vehiculos/9/documentos/perdido.pdf"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("File not found");
    }

    @Test
    void leerArchivo_conFalloDelProveedor_envuelveElMensajeSinReventarPorFaltaDeDetalles() {
        when(s3.getObject(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder()
                        .awsErrorDetails(AwsErrorDetails.builder().errorMessage("AccessDenied").build())
                        .build());

        assertThatThrownBy(() -> almacenamiento.leerArchivo("vehiculos/9/documentos/negado.pdf"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("AccessDenied");
    }

    @Test
    void guardarArchivo_siElProveedorFalla_avisaConElMensaje() {
        when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().build());

        MockMultipartFile archivo = new MockMultipartFile("file", "manual.pdf", "application/pdf",
                "x".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> almacenamiento.guardarArchivo(archivo, "vehiculos/1/documentos"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("almacén de objetos");
    }

    @Test
    void eliminarArchivo_borraPorClave() throws IOException {
        almacenamiento.eliminarArchivo("vehiculos/3/documentos/manual.pdf");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3).deleteObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(captor.getValue().key()).isEqualTo("vehiculos/3/documentos/manual.pdf");
    }

    @Test
    void disponible_esTrueSiElBucketRespondeYFalseSiNo() {
        when(s3.headBucket(any(HeadBucketRequest.class))).thenReturn(null);
        assertThat(almacenamiento.disponible()).isTrue();

        when(s3.headBucket(any(HeadBucketRequest.class))).thenThrow(S3Exception.builder().build());
        assertThat(almacenamiento.disponible()).isFalse();
    }

    @Test
    void bucketInvalido_oVacio_fallaAlConstruir() {
        assertThatThrownBy(() -> new AlmacenamientoS3("", s3))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bucket");
        assertThatThrownBy(() -> new AlmacenamientoS3("Bucket_Con_Mayusculas", s3))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bucket");
    }

    private static ResponseInputStream<GetObjectResponse> respuestaCon(String contenido) {
        return new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                AbortableInputStream.create(new ByteArrayInputStream(contenido.getBytes(StandardCharsets.UTF_8))));
    }
}
