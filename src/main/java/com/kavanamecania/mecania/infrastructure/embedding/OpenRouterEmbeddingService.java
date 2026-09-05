package com.kavanamecania.mecania.infrastructure.embedding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kavanamecania.mecania.domain.embedding.Embedding;
import com.kavanamecania.mecania.domain.embedding.EmbeddingException;
import com.kavanamecania.mecania.domain.embedding.EmbeddingService;
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
 * Implementación de EmbeddingService que llama a OpenRouter.
 *
 * <p>OpenRouter expone un endpoint compatible con OpenAI:
 * POST {baseUrl}/embeddings
 * Body: { "model": "...", "input": "..." }
 * Auth: Authorization: Bearer ***
 * Response: { "data": [ { "embedding": [...floats...], "index": 0 } ] }
 *
 * <p>La API key se resuelve UNA vez en el constructor, con una única fuente de
 * verdad: la property Spring {@code mecania.embedding.api-key} (application.properties,
 * {@code -D} o env var mapeada) y, si está vacía, fallback a la env var
 * {@code OPENROUTER_API_KEY}. No se usa {@code System.getProperty} en cada
 * llamada: Spring NO copia application.properties a system properties, así que
 * esa vía era una trampa silenciosa (configurar la key en application.properties
 * no funcionaba).</p>
 */
@Component
public class OpenRouterEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterEmbeddingService.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String model;
    private final int dimension;
    private final String apiKey;

    public OpenRouterEmbeddingService(
            @Value("${mecania.embedding.base-url:https://openrouter.ai/api/v1}") String baseUrl,
            @Value("${mecania.embedding.model:text-embedding-3-small}") String model,
            @Value("${mecania.embedding.dimension:1536}") int dimension,
            @Value("${mecania.embedding.api-key:}") String apiKeyProperty,
            RestTemplate embeddingRestTemplate) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.dimension = dimension;
        this.restTemplate = embeddingRestTemplate;

        // Una única fuente de verdad, resuelta en el constructor.
        if (apiKeyProperty == null || apiKeyProperty.isBlank()) {
            this.apiKey = System.getenv("OPENROUTER_API_KEY");
        } else {
            this.apiKey = apiKeyProperty;
        }

        if (this.apiKey == null || this.apiKey.isBlank()) {
            log.warn("mecania.embedding.api-key no configurada. OpenRouterEmbeddingService " +
                    "fallará al intentar calcular embeddings hasta que se configure (property o OPENROUTER_API_KEY).");
        }
    }

    @Override
    public Embedding embed(String texto) throws EmbeddingException {
        if (texto == null || texto.isBlank()) {
            throw new EmbeddingException("Texto nulo o vacío");
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new EmbeddingException(
                    "No hay API key de OpenRouter configurada. Define mecania.embedding.api-key " +
                    "o OPENROUTER_API_KEY en el entorno.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", model,
                "input", texto
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<EmbeddingResponse> response = restTemplate.exchange(
                    baseUrl + "/embeddings",
                    org.springframework.http.HttpMethod.POST,
                    request,
                    EmbeddingResponse.class
            );
            EmbeddingResponse respBody = response.getBody();
            if (respBody == null || respBody.data() == null || respBody.data().isEmpty()) {
                throw new EmbeddingException("Respuesta vacía del proveedor de embeddings");
            }
            double[] valores = respBody.data().get(0).embedding();
            if (valores == null || valores.length == 0) {
                throw new EmbeddingException("Embedding vacío en la respuesta");
            }
            if (valores.length != dimension) {
                log.warn("La dimensión del embedding ({}) no coincide con la configurada ({}). " +
                        "Actualiza mecania.embedding.dimension o cambia el modelo.", valores.length, dimension);
            }
            return new Embedding(valores);
        } catch (RestClientException e) {
            throw new EmbeddingException("Error llamando al proveedor de embeddings: " + e.getMessage(), e);
        }
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EmbeddingResponse(List<EmbeddingData> data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EmbeddingData(int index, @JsonProperty("embedding") double[] embedding) {}
}