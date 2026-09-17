package com.kavanamecania.mecania.infrastructure.storage;

import com.kavanamecania.mecania.domain.storage.AlmacenamientoArchivos;
import com.kavanamecania.mecania.domain.storage.NombresArchivo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Almacenamiento de manuales en un servicio de objetos compatible con S3
 * (Cloudflare R2 en producción).
 *
 * <p>Motivo: el disco del contenedor es efímero. Cada redespliegue borraba los
 * manuales mientras la base de datos seguía guardando documentos, fragmentos,
 * vectores y conversaciones que los citaban: descargas en 404 y respuestas RAG
 * apoyadas en una fuente que ya no existía (ADR-003 prometía este cambio y no se
 * había hecho).</p>
 *
 * <p>Se activa con <code>storage.tipo=s3</code>; con cualquier otro valor (o sin
 * valor) manda el almacenamiento en disco local.</p>
 */
@Component
@ConditionalOnProperty(name = "mecania.storage.tipo", havingValue = "s3")
public class AlmacenamientoS3 implements AlmacenamientoArchivos {

    private static final Logger log = LoggerFactory.getLogger(AlmacenamientoS3.class);

    private final S3Client s3;
    private final String bucket;

    @Autowired
    public AlmacenamientoS3(
            @Value("${mecania.storage.s3.bucket:}") String bucket,
            @Value("${mecania.storage.s3.endpoint:}") String endpoint,
            @Value("${mecania.storage.s3.region:auto}") String region,
            @Value("${mecania.storage.s3.access-key:}") String accessKey,
            @Value("${mecania.storage.s3.secret-key:}") String secretKey,
            @Value("${mecania.storage.s3.path-style:false}") boolean pathStyle) {
        this(bucket, construirCliente(endpoint, region, accessKey, secretKey, pathStyle));
    }

    /** Constructor para tests: recibe el cliente ya construido. */
    AlmacenamientoS3(String bucket, S3Client s3) {
        NombresArchivo.validarBucket(bucket);
        this.bucket = bucket;
        this.s3 = s3;
    }

    private static S3Client construirCliente(String endpoint, String region,
                                             String accessKey, String secretKey, boolean pathStyle) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("mecania.storage.s3.endpoint no está configurado");
        }
        if (accessKey == null || accessKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("Faltan las credenciales del almacén de objetos: access-key y secret-key son obligatorias con el almacenamiento S3");
        }
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .forcePathStyle(pathStyle)
                .build();
    }

    @Override
    public String guardarArchivo(MultipartFile file, String subdirectorio) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        String clave = NombresArchivo.clave(file.getOriginalFilename(), subdirectorio);
        String tipo = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        PutObjectRequest peticion = PutObjectRequest.builder()
                .bucket(bucket)
                .key(clave)
                .contentType(tipo)
                .contentLength(file.getSize())
                .build();

        try (InputStream entrada = file.getInputStream()) {
            s3.putObject(peticion, RequestBody.fromInputStream(entrada, file.getSize()));
        } catch (S3Exception e) {
            throw new IOException("No se pudo subir " + clave + " al almacén de objetos: " + detalle(e), e);
        }
        return clave;
    }

    @Override
    public String guardarArchivo(byte[] contenido, String nombreArchivo, String subdirectorio) throws IOException {
        if (contenido == null || contenido.length == 0) {
            throw new IllegalArgumentException("File is empty");
        }
        String clave = NombresArchivo.clave(nombreArchivo, subdirectorio);
        PutObjectRequest peticion = PutObjectRequest.builder()
                .bucket(bucket)
                .key(clave)
                .contentLength((long) contenido.length)
                .build();
        try {
            s3.putObject(peticion, RequestBody.fromBytes(contenido));
        } catch (S3Exception e) {
            throw new IOException("No se pudo subir " + clave + " al almacén de objetos: " + detalle(e), e);
        }
        return clave;
    }

    @Override
    public byte[] leerArchivo(String rutaRelativa) throws IOException {
        try (InputStream entrada = leerArchivoStream(rutaRelativa)) {
            return entrada.readAllBytes();
        }
    }

    /**
     * Descarga en flujo, sin traerse el objeto entero a memoria: en un plan de
     * 384 MB de heap un manual de 25 MB leído de golpe más los búferes de la
     * respuesta HTTP es justo el tipo de cosa que revienta la JVM.
     */
    @Override
    public InputStream leerArchivoStream(String rutaRelativa) throws IOException {
        try {
            ResponseInputStream<GetObjectResponse> respuesta = s3.getObject(
                    GetObjectRequest.builder().bucket(bucket).key(rutaRelativa).build());
            return respuesta;
        } catch (NoSuchKeyException e) {
            throw new IOException("File not found: " + rutaRelativa, e);
        } catch (S3Exception e) {
            throw new IOException("No se pudo leer " + rutaRelativa + " del almacén de objetos: " + detalle(e), e);
        }
    }

    @Override
    public void eliminarArchivo(String rutaRelativa) throws IOException {
        try {
            // DeleteObject es idempotente en S3: borrar algo que no existe no es error.
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(rutaRelativa).build());
        } catch (S3Exception e) {
            throw new IOException("No se pudo borrar " + rutaRelativa + " del almacén de objetos: " + detalle(e), e);
        }
    }

    /**
     * Mensaje del proveedor, sin asumir que venga: una S3Exception construida a
     * mano (o por un fallo de red) no siempre trae detalles y no queremos que el
     * propio manejo del error reviente con un NullPointerException.
     */
    private static String detalle(S3Exception e) {
        return e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage();
    }

    /** El bucket responde. Es lo que mira {@code /health/ready}. */
    @Override
    public boolean disponible() {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            return true;
        } catch (Exception e) {
            log.warn("Almacén de objetos no disponible (bucket {}): {}", bucket, e.getMessage());
            return false;
        }
    }
}