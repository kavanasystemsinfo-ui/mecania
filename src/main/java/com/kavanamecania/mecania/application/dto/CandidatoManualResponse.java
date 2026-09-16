package com.kavanamecania.mecania.application.dto;

import com.kavanamecania.mecania.domain.busqueda.CandidatoManual;

public record CandidatoManualResponse(
        String titulo,
        String url,
        String snippet,
        String fuente,
        boolean pdf
) {
    public static CandidatoManualResponse from(CandidatoManual candidato) {
        return new CandidatoManualResponse(
                candidato.titulo(),
                candidato.url(),
                candidato.snippet(),
                candidato.fuente(),
                candidato.pdf());
    }
}
