package com.kavanamecania.mecania.controller;

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
