package com.kavanamecania.mecania.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kavanamecania.mecania.application.dto.VehiculoRequest;
import com.kavanamecania.mecania.config.SecurityConfig;
import com.kavanamecania.mecania.domain.model.Combustible;
import com.kavanamecania.mecania.domain.model.Vehiculo;
import com.kavanamecania.mecania.domain.service.VehiculoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = VehiculoController.class)
@Import(SecurityConfig.class)
class VehiculoControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockBean VehiculoService service;

    @Test
    void post_crea_vehiculo_y_devuelve_201_con_location() throws Exception {
        when(service.create(any(VehiculoRequest.class))).thenAnswer(inv -> {
            Vehiculo v = Vehiculo.builder()
                    .id(7L).usuarioId(1L)
                    .marca("Renault").modelo("Clio").anio(2020)
                    .combustible(Combustible.GASOLINA).kilometraje(15000L)
                    .matricula("9999ZZZ").build();
            return v;
        });

        String body = om.writeValueAsString(new VehiculoRequest(
                1L, "Renault", "Clio", 2020, Combustible.GASOLINA, 15000L, "9999ZZZ"));

        mvc.perform(post("/api/vehiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/vehiculos/7"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.marca").value("Renault"));
    }

    @Test
    void post_validacion_falla_devuelve_400_con_fields() throws Exception {
        String body = """
                {"usuarioId":null,"marca":"","modelo":"","anio":1500,"combustible":null}
                """;

        mvc.perform(post("/api/vehiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validacion"))
                .andExpect(jsonPath("$.fields").isMap());
    }

    @Test
    void get_por_id_existente_devuelve_200() throws Exception {
        when(service.findById(5L)).thenReturn(Vehiculo.builder()
                .id(5L).usuarioId(2L).marca("Seat").modelo("Ibiza").anio(2019)
                .combustible(Combustible.DIESEL).build());

        mvc.perform(get("/api/vehiculos/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.marca").value("Seat"));
    }

    @Test
    void get_por_id_inexistente_devuelve_404() throws Exception {
        when(service.findById(404L))
                .thenThrow(new com.kavanamecania.mecania.domain.exception.VehiculoNotFoundException(404L));

        mvc.perform(get("/api/vehiculos/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("vehiculo_no_encontrado"));
    }

    @Test
    void get_lista_filtrada_por_usuarioId() throws Exception {
        when(service.findByUsuarioId(1L)).thenReturn(List.of(
                Vehiculo.builder().id(1L).usuarioId(1L).marca("A").modelo("B").anio(2020)
                        .combustible(Combustible.GASOLINA).build()));

        mvc.perform(get("/api/vehiculos").param("usuarioId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioId").value(1));
    }
}
