package com.kavanamecania.mecania.infrastructure.chat;

import com.kavanamecania.mecania.domain.chat.LlmException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OpenRouterLlmServiceTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private OpenRouterLlmService service;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        service = new OpenRouterLlmService(
                "https://api.test.com/v1",
                "openai/gpt-4o-mini",
                800,
                "test-api-key",
                restTemplate
        );
    }

    @Test
    void responder_pide_max_tokens_acotado_en_vez_del_maximo_del_modelo() throws LlmException {
        // Sin max_tokens, OpenRouter reserva el máximo del modelo (16.384 en
        // gpt-4o-mini) y responde 402 si el saldo no cubre esa reserva: el chat
        // quedaba muerto con cualquier saldo pequeño.
        mockServer.expect(requestTo("https://api.test.com/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"model\":\"openai/gpt-4o-mini\",\"max_tokens\":800}", false))
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"ok\"}}]}",
                        MediaType.APPLICATION_JSON));

        assertThat(service.responder("sistema", "usuario")).isEqualTo("ok");
        mockServer.verify();
    }

    @Test
    void responder_sin_saldo_del_proveedor_lanza_llm_exception() {
        mockServer.expect(requestTo("https://api.test.com/v1/chat/completions"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.PAYMENT_REQUIRED)
                        .body("{\"error\":{\"message\":\"This request requires more credits\"}}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.responder("sistema", "usuario"))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("proveedor de chat");
        mockServer.verify();
    }
}
