package com.kavanamecania.mecania.controller;

import com.kavanamecania.mecania.domain.exception.ArchivoDemasiadoGrandeException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contrato de errores HTTP del proyecto. Se prueban las traducciones completas
 * (código de estado + código de error + mensaje) sin arrancar el contexto: lo
 * que se verifica aquí es el mapeo, no el límite real del servidor de
 * aplicaciones (ese se comprueba en producción con curl).
 */
class GlobalExceptionHandlerTest {

    private static final long LIMITE = 25L * 1024 * 1024;
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(LIMITE);

    @Test
    void archivoDemasiadoGrande_devuelve413ConCodigoPropio() {
        ResponseEntity<Map<String, Object>> respuesta =
                handler.archivoDemasiadoGrande(new ArchivoDemasiadoGrandeException("26 MB superan el límite"));

        assertThat(respuesta.getStatusCode().value()).isEqualTo(413);
        assertThat(respuesta.getBody()).containsEntry("error", "archivo_demasiado_grande");
        assertThat(respuesta.getBody().get("message").toString()).contains("26 MB");
    }

    @Test
    void subidaQueCortaSpring_da413YNoFiltraLaClaseDeLaExcepcion() {
        ResponseEntity<Map<String, Object>> respuesta =
                handler.subidaExcedeElLimite(new MaxUploadSizeExceededException(LIMITE));

        assertThat(respuesta.getStatusCode().value()).isEqualTo(413);
        assertThat(respuesta.getBody()).containsEntry("error", "archivo_demasiado_grande");
        String mensaje = respuesta.getBody().get("message").toString();
        assertThat(mensaje).contains("25 MB");
        assertThat(mensaje).doesNotContain("MaxUploadSizeExceededException");
    }

    @Test
    void rutaInexistente_devuelve404YNo500() {
        ResponseEntity<Map<String, Object>> respuesta =
                handler.recursoNoEncontrado(new NoResourceFoundException(HttpMethod.GET, "no-existe"));

        assertThat(respuesta.getStatusCode().value()).isEqualTo(404);
        assertThat(respuesta.getBody()).containsEntry("error", "recurso_no_encontrado");
        assertThat(respuesta.getBody().get("message").toString()).doesNotContain("NoResourceFoundException");
    }

    @Test
    void errorInesperado_devuelve500SinFiltrarInternos() {
        ResponseEntity<Map<String, Object>> respuesta =
                handler.general(new IllegalStateException("detalle interno que no debe salir"));

        assertThat(respuesta.getStatusCode().value()).isEqualTo(500);
        assertThat(respuesta.getBody()).containsEntry("error", "error_interno");
        String mensaje = respuesta.getBody().get("message").toString();
        assertThat(mensaje).doesNotContain("IllegalStateException");
        assertThat(mensaje).doesNotContain("detalle interno");
    }
}
