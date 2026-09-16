package com.kavanamecania.mecania.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kavanamecania.mecania.application.dto.VehiculoRequest;
import com.kavanamecania.mecania.domain.model.Combustible;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class VehiculoControllerIT {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper om;

    private MockMvc mvc() {
        return MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void flujo_completo_post_get_put_delete() throws Exception {
        MockMvc mvc = mvc();

        // POST (el propietario lo asigna el token; en test auth deshabilitada es el usuario demo 1)
        String body = om.writeValueAsString(new VehiculoRequest(
                "Ford", "Focus", 2017, Combustible.DIESEL, 120000L, "5555XXX"));
        MvcResult postResult = mvc.perform(post("/api/vehiculos")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        Long id = om.readTree(postResult.getResponse().getContentAsString()).get("id").asLong();

        // GET
        mvc.perform(get("/api/vehiculos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marca").value("Ford"))
                .andExpect(jsonPath("$.combustible").value("DIESEL"));

        // PUT
        String updated = om.writeValueAsString(new VehiculoRequest(
                "Ford", "Focus", 2018, Combustible.DIESEL, 130000L, "5555XXX"));
        mvc.perform(put("/api/vehiculos/" + id)
                        .contentType(MediaType.APPLICATION_JSON).content(updated))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anio").value(2018))
                .andExpect(jsonPath("$.kilometraje").value(130000));

        // DELETE
        mvc.perform(delete("/api/vehiculos/" + id))
                .andExpect(status().isNoContent());

        // GET tras delete: 404
        mvc.perform(get("/api/vehiculos/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void post_duplicado_devuelve_409() throws Exception {
        MockMvc mvc = mvc();
        String body = om.writeValueAsString(new VehiculoRequest(
                "Peugeot", "308", 2019, Combustible.GASOLINA, 50000L, null));

        mvc.perform(post("/api/vehiculos")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/vehiculos")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("vehiculo_duplicado"));
    }

    @Test
    void list_devuelve_solo_los_del_usuario_autenticado() throws Exception {
        MockMvc mvc = mvc();

        mvc.perform(post("/api/vehiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new VehiculoRequest(
                                "Audi", "A3", 2020, Combustible.GASOLINA, 30000L, null))))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/vehiculos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioId").value(1));
    }
}
