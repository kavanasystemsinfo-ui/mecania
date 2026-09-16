package com.kavanamecania.mecania.application.dto;

import com.kavanamecania.mecania.domain.vector.FragmentoSimilar;

import java.util.List;

/**
 * Respuesta del asistente. {@code sinBase} distingue "no tengo información en
 * los manuales de este vehículo" (respuesta legítima, sin llamar al LLM) de una
 * respuesta con fuentes verificables.
 */
public record RespuestaChat(
        String respuesta,
        boolean sinBase,
        List<FuenteRespuesta> fuentes
) {
    public static RespuestaChat sinInformacion() {
        return new RespuestaChat(
                "No tengo información en los manuales de este vehículo para responder a eso.",
                true,
                List.of());
    }

    public static RespuestaChat conFuentes(String respuesta, List<FragmentoSimilar> fuentes) {
        return new RespuestaChat(
                respuesta,
                false,
                fuentes.stream().map(FuenteRespuesta::from).toList());
    }
}
