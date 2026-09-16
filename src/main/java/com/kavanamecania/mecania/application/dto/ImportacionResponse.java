package com.kavanamecania.mecania.application.dto;

import com.kavanamecania.mecania.application.ResultadoImportacion;

public record ImportacionResponse(DocumentoResponse documento, boolean yaExistia) {

    public static ImportacionResponse from(ResultadoImportacion resultado) {
        return new ImportacionResponse(
                DocumentoResponse.from(resultado.documento()),
                resultado.yaExistia());
    }
}
