package com.kavanamecania.mecania.controller;

import com.kavanamecania.mecania.domain.busqueda.BusquedaException;
import com.kavanamecania.mecania.domain.descarga.DescargaException;
import com.kavanamecania.mecania.domain.descarga.MotivoDescarga;
import com.kavanamecania.mecania.domain.exception.VehiculoDuplicadoException;
import com.kavanamecania.mecania.domain.exception.VehiculoNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(VehiculoNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(VehiculoNotFoundException ex) {
        return body(HttpStatus.NOT_FOUND, "vehiculo_no_encontrado", ex.getMessage(), null);
    }

    @ExceptionHandler(VehiculoDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> duplicado(VehiculoDuplicadoException ex) {
        return body(HttpStatus.CONFLICT, "vehiculo_duplicado", ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        f -> f.getField(),
                        f -> f.getDefaultMessage() == null ? "inválido" : f.getDefaultMessage(),
                        (a, b) -> a));
        return body(HttpStatus.BAD_REQUEST, "validacion", "Datos inválidos", fields);
    }

    /**
     * La búsqueda externa falló (bloqueo anti-bot, error HTTP o caída de red).
     * Se responde 503 con mensaje explícito: el usuario debe poder distinguir
     * "el buscador no responde" de "no hay resultados".
     */
    @ExceptionHandler(BusquedaException.class)
    public ResponseEntity<Map<String, Object>> busquedaNoDisponible(BusquedaException ex) {
        return body(HttpStatus.SERVICE_UNAVAILABLE, "buscador_no_disponible", ex.getMessage(), null);
    }

    /** Descarga de un manual rechazada o fallida: código según el motivo. */
    @ExceptionHandler(DescargaException.class)
    public ResponseEntity<Map<String, Object>> descargaFallida(DescargaException ex) {
        String codigo = switch (ex.motivo()) {
            case URL_INVALIDA -> "url_no_valida";
            case HOST_NO_PERMITIDO -> "host_no_permitido";
            case TIPO_NO_SOPORTADO -> "tipo_no_soportado";
            case DEMASIADO_GRANDE -> "demasiado_grande";
            case ERROR_RED -> "descarga_fallida";
        };
        return body(estadoDeDescarga(ex.motivo()), codigo, ex.getMessage(), null);
    }

    private static HttpStatus estadoDeDescarga(MotivoDescarga motivo) {
        return switch (motivo) {
            case URL_INVALIDA, HOST_NO_PERMITIDO -> HttpStatus.BAD_REQUEST;
            case TIPO_NO_SOPORTADO -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            case DEMASIADO_GRANDE -> HttpStatus.PAYLOAD_TOO_LARGE;
            case ERROR_RED -> HttpStatus.BAD_GATEWAY;
        };
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> dataIntegrity(DataIntegrityViolationException ex) {
        return body(HttpStatus.CONFLICT, "integridad_datos",
                "Restricción de integridad violada", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> general(Exception ex) {
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "error_interno",
                ex.getClass().getSimpleName() + ": " + ex.getMessage(), null);
    }

    private static ResponseEntity<Map<String, Object>> body(
            HttpStatus status, String code, String message, Map<String, String> fields) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("timestamp", Instant.now().toString());
        map.put("status", status.value());
        map.put("error", code);
        map.put("message", message);
        if (fields != null) {
            map.put("fields", fields);
        }
        return ResponseEntity.status(status).body(map);
    }
}
