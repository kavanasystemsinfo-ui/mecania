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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato HTTP de autenticación con la seguridad REAL activa
 * (mecania.auth.enabled=true sobre H2).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test-auth")
class AuthControllerIT {

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

    @Test
    void registro_devuelve_201_y_token() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@test.com\",\"password\":\"secreto123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value("a@test.com"));
    }

    @Test
    void registro_duplicado_devuelve_409() throws Exception {
        registrar("dup@test.com", "secreto123");
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dup@test.com\",\"password\":\"secreto123\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("email_ya_registrado"));
    }

    @Test
    void login_correcto_devuelve_200_y_token() throws Exception {
        registrar("ok@test.com", "secreto123");
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ok@test.com\",\"password\":\"secreto123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void login_password_incorrecta_devuelve_401() throws Exception {
        registrar("bad@test.com", "secreto123");
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bad@test.com\",\"password\":\"otra12345\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("credenciales_invalidas"));
    }

    @Test
    void endpoint_protegido_sin_token_devuelve_401() throws Exception {
        mvc.perform(get("/api/vehiculos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpoint_protegido_con_token_devuelve_200() throws Exception {
        String token = registrar("aut@test.com", "secreto123");
        mvc.perform(get("/api/vehiculos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
