package com.kavanamecania.mecania.controller;

import com.kavanamecania.mecania.config.DiagnosticoEntorno;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /health/ready: el endpoint honesto. A diferencia de /health (que solo mira la
 * base de datos), aquí un despliegue sin clave de embeddings o con el
 * almacenamiento no escribible NO puede salir en verde: la IA estaría muerta
 * con el servicio diciendo que todo va bien (pasó el 2026-09-17 en Render).
 */
class HealthReadyIT {

    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = "mecania.embedding.api-key=clave-de-prueba")
    @Nested
    class ConDependenciasOk {

        @Autowired private WebApplicationContext context;

        @Test
        void devuelve200YElDetalleDeCadaComprobacion() throws Exception {
            MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();

            mvc.perform(get("/health/ready"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.checks.baseDatos").value("UP"))
                    .andExpect(jsonPath("$.checks.claveEmbeddings").value("UP"))
                    .andExpect(jsonPath("$.checks.almacenamiento").value("UP"));
        }
    }

    @SpringBootTest
    @ActiveProfiles("test")
    @Nested
    class Bloqueado {

        @Autowired private WebApplicationContext context;

        @MockBean private DiagnosticoEntorno diagnostico;

        @Test
        void sinClaveDeEmbeddings_devuelve503YLoDice() throws Exception {
            when(diagnostico.baseDatosOk()).thenReturn(true);
            when(diagnostico.almacenamientoEscribible()).thenReturn(true);
            when(diagnostico.claveEmbeddingsPresente()).thenReturn(false);
            MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();

            mvc.perform(get("/health/ready"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value("DOWN"))
                    .andExpect(jsonPath("$.checks.claveEmbeddings").value("DOWN"))
                    .andExpect(jsonPath("$.checks.baseDatos").value("UP"));
        }

        @Test
        void conAlmacenamientoNoEscribible_devuelve503() throws Exception {
            when(diagnostico.baseDatosOk()).thenReturn(true);
            when(diagnostico.claveEmbeddingsPresente()).thenReturn(true);
            when(diagnostico.almacenamientoEscribible()).thenReturn(false);
            MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();

            mvc.perform(get("/health/ready"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.checks.almacenamiento").value("DOWN"));
        }
    }
}
