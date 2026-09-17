package com.kavanamecania.mecania.controller;

import com.kavanamecania.mecania.config.DiagnosticoEntorno;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Estado del servicio.
 *
 * <ul>
 *   <li>{@code /health}: liveness rápido (solo base de datos). Es el que usa
 *       Render para dar por vivo un despliegue.</li>
 *   <li>{@code /health/ready}: readiness honesto. Comprueba base de datos,
 *       clave de embeddings y almacenamiento escribible. Si la IA no puede
 *       funcionar, aquí se dice: el 2026-09-17 /health devolvía UP con el
 *       pipeline de embeddings muerto.</li>
 * </ul>
 *
 * <p>Ninguna de las dos rutas requiere autenticación: están fuera de
 * {@code /api/**} y las usan los sistemas de despliegue.</p>
 */
@RestController
public class HealthController {

    private final DiagnosticoEntorno diagnostico;

    public HealthController(DiagnosticoEntorno diagnostico) {
        this.diagnostico = diagnostico;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return diagnostico.baseDatosOk()
                ? ResponseEntity.ok(Map.of("status", "UP"))
                : ResponseEntity.status(503).body(Map.of("status", "DOWN"));
    }

    @GetMapping("/health/ready")
    public ResponseEntity<Map<String, Object>> ready() {
        boolean baseDatos = diagnostico.baseDatosOk();
        boolean claveEmbeddings = diagnostico.claveEmbeddingsPresente();
        boolean almacenamiento = diagnostico.almacenamientoEscribible();

        Map<String, String> comprobaciones = new LinkedHashMap<>();
        comprobaciones.put("baseDatos", baseDatos ? "UP" : "DOWN");
        comprobaciones.put("claveEmbeddings", claveEmbeddings ? "UP" : "DOWN");
        comprobaciones.put("almacenamiento", almacenamiento ? "UP" : "DOWN");

        boolean todoOk = baseDatos && claveEmbeddings && almacenamiento;
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("status", todoOk ? "UP" : "DOWN");
        cuerpo.put("checks", comprobaciones);

        return ResponseEntity.status(todoOk ? 200 : 503).body(cuerpo);
    }
}

