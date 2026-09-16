package com.kavanamecania.mecania.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kavanamecania.mecania.application.dto.AlertaRequest;
import com.kavanamecania.mecania.application.dto.VehiculoRequest;
import com.kavanamecania.mecania.domain.model.Combustible;
import com.kavanamecania.mecania.domain.model.Repetitividad;
import com.kavanamecania.mecania.domain.model.TipoAlerta;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class AlertaControllerIT {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper om;

    private MockMvc mvc() {
        return MockMvcBuilders.webAppContextSetup(context).build();
    }

    private Long crearVehiculo(MockMvc mvc, Long usuarioId, String marca) throws Exception {
        String body = om.writeValueAsString(new VehiculoRequest(
                usuarioId, marca, "Modelo", 2020, Combustible.GASOLINA, 10000L, null));
        MvcResult r = mvc.perform(post("/api/vehiculos")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return om.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private String alertaBody(TipoAlerta tipo, String desc, LocalDate fecha, Repetitividad rep)
            throws Exception {
        return om.writeValueAsString(new AlertaRequest(tipo, desc, fecha, rep));
    }

    @Test
    void crear_listar_actualizar_y_eliminar() throws Exception {
        MockMvc mvc = mvc();
        Long vid = crearVehiculo(mvc, 700L, "Seat");

        // crear
        MvcResult crear = mvc.perform(post("/api/vehiculos/" + vid + "/alertas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alertaBody(TipoAlerta.ITV, "ITV", LocalDate.now().plusDays(30), Repetitividad.ANUAL)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.tipo").value("ITV"))
                .andExpect(jsonPath("$.activa").value(true))
                .andReturn();
        Long alertaId = om.readTree(crear.getResponse().getContentAsString()).get("id").asLong();

        // listar
        mvc.perform(get("/api/vehiculos/" + vid + "/alertas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(alertaId));

        // actualizar
        mvc.perform(put("/api/vehiculos/" + vid + "/alertas/" + alertaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alertaBody(TipoAlerta.FRENOS, "Frenos", LocalDate.now().plusDays(10), Repetitividad.UNICA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("FRENOS"))
                .andExpect(jsonPath("$.descripcion").value("Frenos"));

        // actualizar no encontrada -> 404
        mvc.perform(put("/api/vehiculos/" + vid + "/alertas/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alertaBody(TipoAlerta.FRENOS, "Frenos", LocalDate.now().plusDays(10), Repetitividad.UNICA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("alerta_no_encontrada"));

        // eliminar -> 204
        mvc.perform(delete("/api/vehiculos/" + vid + "/alertas/" + alertaId))
                .andExpect(status().isNoContent());
    }

    @Test
    void vencidas_conAlertaVencida_devuelveYDesactiva() throws Exception {
        MockMvc mvc = mvc();
        Long vid = crearVehiculo(mvc, 701L, "Renault");

        // crear alerta UNICA vencida (ayer)
        MvcResult crear = mvc.perform(post("/api/vehiculos/" + vid + "/alertas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alertaBody(TipoAlerta.ACEITE, "Aceite", LocalDate.now().minusDays(1), Repetitividad.UNICA)))
                .andExpect(status().isCreated())
                .andReturn();
        Long alertaId = om.readTree(crear.getResponse().getContentAsString()).get("id").asLong();

        // vencidas -> 200 con la alerta desactivada
        mvc.perform(get("/api/vehiculos/" + vid + "/alertas/vencidas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(alertaId))
                .andExpect(jsonPath("$[0].activa").value(false));

        // la alerta queda desactivada al listar
        mvc.perform(get("/api/vehiculos/" + vid + "/alertas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].activa").value(false));
    }
}
