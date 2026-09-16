package com.kavanamecania.mecania.application;

import com.kavanamecania.mecania.application.dto.RespuestaChat;
import com.kavanamecania.mecania.domain.chat.LlmException;
import com.kavanamecania.mecania.domain.chat.LlmService;
import com.kavanamecania.mecania.domain.embedding.Embedding;
import com.kavanamecania.mecania.domain.embedding.EmbeddingException;
import com.kavanamecania.mecania.domain.embedding.EmbeddingService;
import com.kavanamecania.mecania.domain.vector.FragmentoSimilar;
import com.kavanamecania.mecania.domain.vector.RepositorioVectores;
import com.kavanamecania.mecania.domain.vector.VectorPersistenceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Chat RAG del vehículo: vectoriza la pregunta, recupera los fragmentos de
 * manual más parecidos (solo de ESE vehículo) y responde con el LLM citando las
 * fuentes.
 *
 * <p>Reglas de honestidad: si no hay fragmentos relevantes por encima de la
 * similitud mínima, se responde "sin base" SIN llamar al LLM (el modelo nunca
 * recibe una pregunta sin contexto y no puede inventar). Las fuentes devueltas
 * son exactamente las que se inyectaron en el prompt.</p>
 */
@Service
public class ChatManualesService {

    private static final String PROMPT_SISTEMA = """
            Eres un asistente de mecánica. Responde ÚNICAMENTE con la información de los \
            fragmentos de manual que se te proporcionan. Si los fragmentos no contienen la \
            respuesta, dilo claramente y no inventes. Responde en español, de forma breve y \
            concreta, y menciona de qué manual sale la información cuando sea útil.
            """;

    private final EmbeddingService embeddingService;
    private final RepositorioVectores repositorioVectores;
    private final LlmService llmService;
    private final int topK;
    private final double similitudMinima;

    public ChatManualesService(
            EmbeddingService embeddingService,
            RepositorioVectores repositorioVectores,
            LlmService llmService,
            @Value("${mecania.chat.top-k:4}") int topK,
            @Value("${mecania.chat.similitud-minima:0.2}") double similitudMinima) {
        this.embeddingService = embeddingService;
        this.repositorioVectores = repositorioVectores;
        this.llmService = llmService;
        this.topK = topK;
        this.similitudMinima = similitudMinima;
    }

    /**
     * Responde a una pregunta del usuario sobre los manuales del vehículo.
     */
    public RespuestaChat responder(Long vehiculoId, String pregunta)
            throws EmbeddingException, VectorPersistenceException, LlmException {
        Embedding consulta = embeddingService.embed(pregunta);

        List<FragmentoSimilar> relevantes = repositorioVectores.buscarSimilares(vehiculoId, consulta, topK).stream()
                .filter(f -> f.similitud() >= similitudMinima)
                .toList();

        if (relevantes.isEmpty()) {
            return RespuestaChat.sinInformacion();
        }

        String respuesta = llmService.responder(PROMPT_SISTEMA, construirPrompt(relevantes, pregunta));
        return RespuestaChat.conFuentes(respuesta, relevantes);
    }

    private static String construirPrompt(List<FragmentoSimilar> relevantes, String pregunta) {
        StringBuilder contexto = new StringBuilder("Fragmentos de manual del vehículo:\n");
        for (FragmentoSimilar f : relevantes) {
            contexto.append("- [manual ")
                    .append(f.documentoId())
                    .append(", fragmento ")
                    .append(f.posicion())
                    .append("]: ")
                    .append(f.texto())
                    .append('\n');
        }
        contexto.append("\nPregunta del usuario: ").append(pregunta);
        return contexto.toString();
    }
}
