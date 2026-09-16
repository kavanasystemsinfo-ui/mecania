package com.kavanamecania.mecania.controller;

import com.kavanamecania.mecania.application.ChatManualesService;
import com.kavanamecania.mecania.application.dto.PreguntaRequest;
import com.kavanamecania.mecania.application.dto.RespuestaChat;
import com.kavanamecania.mecania.domain.chat.LlmException;
import com.kavanamecania.mecania.domain.embedding.EmbeddingException;
import com.kavanamecania.mecania.domain.vector.VectorPersistenceException;
import com.kavanamecania.mecania.infrastructure.repository.VehiculoRepository;
import com.kavanamecania.mecania.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chat RAG del vehículo: responde preguntas usando SOLO los manuales de ese
 * vehículo. Las respuestas devuelven las fuentes (fragmentos) que las sostienen.
 */
@RestController
@RequestMapping("/api/vehiculos/{vehiculoId}/chat")
@Validated
public class ChatManualesController {

    private final ChatManualesService chatManualesService;
    private final VehiculoRepository vehiculoRepository;

    public ChatManualesController(ChatManualesService chatManualesService,
                                  VehiculoRepository vehiculoRepository) {
        this.chatManualesService = chatManualesService;
        this.vehiculoRepository = vehiculoRepository;
    }

    @PostMapping
    public ResponseEntity<RespuestaChat> preguntar(
            @NotNull @PathVariable Long vehiculoId,
            @Valid @RequestBody PreguntaRequest request)
            throws EmbeddingException, VectorPersistenceException, LlmException {
        if (!vehiculoRepository.existsByIdAndUsuarioId(vehiculoId, SecurityUtils.usuarioIdActual())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(chatManualesService.responder(vehiculoId, request.pregunta()));
    }
}
