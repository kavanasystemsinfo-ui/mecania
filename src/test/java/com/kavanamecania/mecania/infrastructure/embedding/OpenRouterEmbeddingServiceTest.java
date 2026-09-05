package com.kavanamecania.mecania.infrastructure.embedding;

import com.kavanamecania.mecania.domain.embedding.Embedding;
import com.kavanamecania.mecania.domain.embedding.EmbeddingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OpenRouterEmbeddingServiceTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private OpenRouterEmbeddingService service;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        service = new OpenRouterEmbeddingService(
                "https://api.test.com/v1",
                "text-embedding-3-small",
                4,
                "test-api-key",
                restTemplate
        );
        System.setProperty("mecania.embedding.api-key", "test-api-key");
    }

    @Test
    void embed_retorna_vector_con_dimensiones_correctas() throws EmbeddingException {
        mockServer.expect(requestTo("https://api.test.com/v1/embeddings"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(
                        "{\"data\":[{\"index\":0,\"embedding\":[0.1,0.2,0.3,0.4]}]}",
                        MediaType.APPLICATION_JSON));

        Embedding result = service.embed("Hola mundo");

        assertThat(result).isNotNull();
        assertThat(result.dimension()).isEqualTo(4);
        assertThat(result.valores()).containsExactly(0.1, 0.2, 0.3, 0.4);
        mockServer.verify();
    }

    @Test
    void embed_con_texto_vacio_lanza_excepcion() {
        assertThatThrownBy(() -> service.embed(""))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("vacío");
        mockServer.verify(); // sin expectations = no se llamó
    }

    @Test
    void embed_con_texto_null_lanza_excepcion() {
        assertThatThrownBy(() -> service.embed(null))
                .isInstanceOf(EmbeddingException.class);
    }

    @Test
    void embed_con_error_500_lanza_excepcion() {
        mockServer.expect(requestTo("https://api.test.com/v1/embeddings"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> service.embed("texto"))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("Error llamando");
    }

    @Test
    void embed_con_respuesta_vacia_lanza_excepcion() {
        mockServer.expect(requestTo("https://api.test.com/v1/embeddings"))
                .andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.embed("texto"))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("vacía");
    }

    @Test
    void dimension_devuelve_la_dimensión_configurada() {
        assertThat(service.dimension()).isEqualTo(4);
    }

    @Test
    void embed_con_embedding_de_dimensión_diferente_aun_funciona_pero_avisa() throws EmbeddingException {
        // La dimensión del modelo (5) no coincide con la configurada (4). El servicio debe
        // devolver el embedding tal cual y avisar por log, no fallar.
        mockServer.expect(requestTo("https://api.test.com/v1/embeddings"))
                .andRespond(withSuccess(
                        "{\"data\":[{\"index\":0,\"embedding\":[0.1,0.2,0.3,0.4,0.5]}]}",
                        MediaType.APPLICATION_JSON));

        Embedding result = service.embed("texto");

        assertThat(result.dimension()).isEqualTo(5);
        mockServer.verify();
    }
}