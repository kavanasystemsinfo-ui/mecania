package com.kavanamecania.mecania.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Health check de despliegue. Devuelve 200 + {"status":"UP"} solo si la base
 * de datos responde; si no, 503 + {"status":"DOWN"}. No requiere autenticación
 * (Render/Fly hacen ping a esta ruta para saber si el servicio está vivo).
 */
@RestController
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok(Map.of("status", "UP"));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of("status", "DOWN"));
        }
    }
}
