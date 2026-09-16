package com.kavanamecania.mecania.application;

import com.kavanamecania.mecania.application.dto.RespuestaChat;
import com.kavanamecania.mecania.domain.chat.LlmException;
import com.kavanamecania.mecania.domain.chat.LlmService;
import com.kavanamecania.mecania.domain.embedding.Embedding;
import com.kavanamecania.mecania.domain.embedding.EmbeddingService;
import com.kavanamecania.mecania.domain.vector.FragmentoSimilar;
import com.kavanamecania.mecania.domain.vector.RepositorioVectores;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatManualesServiceTest {

    @Mock private EmbeddingService embeddingService;
    @Mock private RepositorioVectores repositorioVectores;
    @Mock private LlmService llmService;

    private ChatManualesService service;

    @BeforeEach
    void setUp() {
        service = new ChatManualesService(embeddingService, repositorioVectores, llmService, 4, 0.2);
    }

    @Test
    void responde_con_los_fragmentos_relevantes_y_devuelve_las_fuentes() throws Exception {
        Embedding consulta = new Embedding(new double[]{0.1, 0.2, 0.3, 0.4});
        when(embeddingService.embed("¿cada cuánto cambio el aceite?")).thenReturn(consulta);

        FragmentoSimilar f1 = new FragmentoSimilar(1L, 10L, 0, "El aceite se cambia cada 15.000 km.", 0.85);
        when(repositorioVectores.buscarSimilares(7L, consulta, 4)).thenReturn(List.of(f1));
        when(llmService.responder(anyString(), anyString())).thenReturn("Cambia el aceite cada 15.000 km.");

        RespuestaChat r = service.responder(7L, "¿cada cuánto cambio el aceite?");

        assertThat(r.sinBase()).isFalse();
        assertThat(r.respuesta()).isEqualTo("Cambia el aceite cada 15.000 km.");
        assertThat(r.fuentes()).hasSize(1);
        assertThat(r.fuentes().get(0).documentoId()).isEqualTo(10L);
        assertThat(r.fuentes().get(0).texto()).contains("aceite");
    }

    @Test
    void sin_fragmentos_relevantes_no_llama_al_llm_y_marca_sin_base() throws Exception {
        when(embeddingService.embed(anyString())).thenReturn(new Embedding(new double[]{0.1, 0.2, 0.3, 0.4}));
        when(repositorioVectores.buscarSimilares(anyLong(), any(Embedding.class), anyInt()))
                .thenReturn(List.of());

        RespuestaChat r = service.responder(7L, "¿qué potencia tiene?");

        assertThat(r.sinBase()).isTrue();
        assertThat(r.fuentes()).isEmpty();
        assertThat(r.respuesta()).contains("No tengo información");
        verify(llmService, never()).responder(anyString(), anyString());
    }

    @Test
    void descarta_los_fragmentos_por_debajo_de_la_similitud_minima() throws Exception {
        when(embeddingService.embed(anyString())).thenReturn(new Embedding(new double[]{0.1, 0.2, 0.3, 0.4}));
        FragmentoSimilar bueno = new FragmentoSimilar(1L, 10L, 0, "Información relevante.", 0.9);
        FragmentoSimilar malo = new FragmentoSimilar(2L, 11L, 1, "Contenido irrelevante.", 0.05);
        when(repositorioVectores.buscarSimilares(anyLong(), any(Embedding.class), anyInt()))
                .thenReturn(List.of(bueno, malo));
        when(llmService.responder(anyString(), anyString())).thenReturn("respuesta");

        service.responder(7L, "pregunta");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(llmService).responder(anyString(), captor.capture());
        assertThat(captor.getValue())
                .contains("Información relevante")
                .doesNotContain("Contenido irrelevante");
    }

    @Test
    void propaga_el_fallo_del_llm() throws Exception {
        when(embeddingService.embed(anyString())).thenReturn(new Embedding(new double[]{0.1, 0.2, 0.3, 0.4}));
        when(repositorioVectores.buscarSimilares(anyLong(), any(Embedding.class), anyInt()))
                .thenReturn(List.of(new FragmentoSimilar(1L, 10L, 0, "texto", 0.9)));
        when(llmService.responder(anyString(), anyString())).thenThrow(new LlmException("proveedor caído"));

        assertThatThrownBy(() -> service.responder(7L, "pregunta"))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("proveedor caído");
    }
}
