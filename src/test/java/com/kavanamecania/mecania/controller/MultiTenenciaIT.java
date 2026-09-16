package com.kavanamecania.mecania.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Multi-tenencia: un usuario autenticado solo ve y toca SUS vehículos.
 * Perfil test-auth (auth real + H2).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test-auth")
class MultiTenenciaIT {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper om;

    private String registrar(String email, String password) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isCreated())
                .andReturn();
        return om.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    private Long crearVehiculo(String token, String marca) throws Exception {
        MvcResult r = mvc.perform(post("/api/vehiculos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "marca", marca, "modelo", "X", "anio", 2020,
                                "combustible", "GASOLINA", "kilometraje", 10000))))
                .andExpect(status().isCreated())
                .andReturn();
        return om.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void usuario_no_ve_vehiculos_ajenos() throws Exception {
        String tokenA = registrar("a@mt.com", "secreto123");
        String tokenB = registrar("b@mt.com", "secreto123");

        Long vehiculoA = crearVehiculo(tokenA, "Toyota");

        // B no puede ver el vehículo de A por id
        mvc.perform(get("/api/vehiculos/" + vehiculoA).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // El listado de B está vacío (B no tiene vehículos propios)
        mvc.perform(get("/api/vehiculos").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        // A sí ve su vehículo
        mvc.perform(get("/api/vehiculos").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].marca").value("Toyota"));
    }

    @Test
    void usuario_no_puede_borrar_vehiculo_ajeno() throws Exception {
        String tokenA = registrar("c@mt.com", "secreto123");
        String tokenB = registrar("d@mt.com", "secreto123");

        Long vehiculoA = crearVehiculo(tokenA, "Seat");

        mvc.perform(delete("/api/vehiculos/" + vehiculoA).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }
}
