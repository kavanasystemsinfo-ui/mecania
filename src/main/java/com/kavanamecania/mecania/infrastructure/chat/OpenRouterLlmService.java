package com.kavanamecania.mecania.infrastructure.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kavanamecania.mecania.domain.chat.LlmException;
import com.kavanamecania.mecania.domain.chat.LlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Implementación de {@link LlmService} que llama al endpoint de chat de
 * OpenRouter (compatible con OpenAI).
 *
 * <p>La API key se resuelve igual que en {@code OpenRouterEmbeddingService}:
 * property {@code mecania.chat.api-key} y, si está vacía, la env var
 * {@code OPENROUTER_API_KEY}. En desarrollo ambos servicios comparten esa env var.</p>
 */
@Component
public class OpenRouterLlmService implements LlmService {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterLlmService.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String model;
    private final String apiKey;

    public OpenRouterLlmService(
            @Value("${mecania.chat.base-url:https://openrouter.ai/api/v1}") String baseUrl,
            @Value("${mecania.chat.model:openai/gpt-4o-mini}") String model,
            @Value("${mecania.chat.api-key:}") String apiKeyProperty,
            RestTemplate chatRestTemplate) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.restTemplate = chatRestTemplate;

        if (apiKeyProperty == null || apiKeyProperty.isBlank()) {
            this.apiKey = System.getenv("OPENROUTER_API_KEY");
        } else {
            this.apiKey = apiKeyProperty;
        }

        if (this.apiKey == null || this.apiKey.isBlank()) {
            log.warn("mecania.chat.api-key no configurada. OpenRouterLlmService fallará "
                    + "hasta que se configure (property o OPENROUTER_API_KEY).");
        }
    }

    @Override
    public String responder(String promptSistema, String promptUsuario) throws LlmException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new LlmException(
                    "No hay API key de OpenRouter configurada. Define mecania.chat.api-key "
                            + "o OPENROUTER_API_KEY en el entorno.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.2,
                "messages", List.of(
                        Map.of("role", "system", "content", promptSistema),
                        Map.of("role", "user", "content", promptUsuario)
                ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<ChatResponse> response = restTemplate.exchange(
                    baseUrl + "/chat/completions",
                    org.springframework.http.HttpMethod.POST,
                    request,
                    ChatResponse.class);
            ChatResponse respBody = response.getBody();
            if (respBody == null || respBody.choices() == null || respBody.choices().isEmpty()
                    || respBody.choices().get(0).message() == null
                    || respBody.choices().get(0).message().content() == null) {
                throw new LlmException("Respuesta vacía del proveedor de chat");
            }
            return respBody.choices().get(0).message().content();
        } catch (RestClientException e) {
            throw new LlmException("Error llamando al proveedor de chat: " + e.getMessage(), e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(List<Choice> choices) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Message message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String content) {}
}
