package com.kavanamecania.mecania.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spec del rate limiting: debe devolver 429 con el mismo contrato de errores que
 * el resto de la API, separar buckets por IP y ser más estricto en el login.
 */
class RateLimitFilterTest {

    private RateLimitProperties props;
    private RateLimitFilter filtro;

    @BeforeEach
    void setUp() {
        props = new RateLimitProperties();
        props.setEnabled(true);
        props.setApiPeticionesPorMinuto(3);   // bucket diminuto para probar sin esperar
        props.setAuthPeticionesPorMinuto(1);
        filtro = new RateLimitFilter(props);
    }

    @Test
    void hasta_el_limite_pasa_y_el_siguiente_devuelve_429() throws Exception {
        for (int i = 0; i < 3; i++) {
            MockHttpServletResponse resp = ejecutar("/api/vehiculos", "10.0.0.1");
            assertThat(resp.getStatus()).isEqualTo(200).describedAs("petición " + (i + 1));
        }
        MockHttpServletResponse cuarta = ejecutar("/api/vehiculos", "10.0.0.1");
        assertThat(cuarta.getStatus()).isEqualTo(429);
        assertThat(cuarta.getContentAsString()).contains("\"error\":\"demasiadas_peticiones\"");
        assertThat(cuarta.getHeader("Retry-After")).isNotNull();
    }

    @Test
    void desactivado_deja_pasar_sin_limite() throws Exception {
        props.setEnabled(false);
        for (int i = 0; i < 20; i++) {
            MockHttpServletResponse resp = ejecutar("/api/vehiculos", "10.0.0.2");
            assertThat(resp.getStatus()).isEqualTo(200);
        }
    }

    @Test
    void ips_distintas_tienen_buckets_independientes() throws Exception {
        // La IP A agota su bucket...
        for (int i = 0; i < 3; i++) {
            ejecutar("/api/vehiculos", "10.0.0.10");
        }
        assertThat(ejecutar("/api/vehiculos", "10.0.0.10").getStatus()).isEqualTo(429);
        // ...pero la IP B sigue pasando.
        assertThat(ejecutar("/api/vehiculos", "10.0.0.20").getStatus()).isEqualTo(200);
    }

    @Test
    void el_login_tiene_un_bucket_mas_estricto() throws Exception {
        assertThat(ejecutar("/api/auth/login", "10.0.0.30").getStatus()).isEqualTo(200);
        assertThat(ejecutar("/api/auth/login", "10.0.0.30").getStatus()).isEqualTo(429);
        // Y no toca el bucket de la API general de la misma IP.
        assertThat(ejecutar("/api/vehiculos", "10.0.0.30").getStatus()).isEqualTo(200);
        assertThat(ejecutar("/api/vehiculos", "10.0.0.30").getStatus()).isEqualTo(200);
        assertThat(ejecutar("/api/vehiculos", "10.0.0.30").getStatus()).isEqualTo(200);
    }

    @Test
    void usaLata_de_forwarded_for_para_la_ip() throws Exception {
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/vehiculos");
            req.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.1");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filtro.doFilter(req, resp, new MockFilterChain());
            assertThat(resp.getStatus()).isEqualTo(200);
        }
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/vehiculos");
        req.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.1");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filtro.doFilter(req, resp, new MockFilterChain());
        assertThat(resp.getStatus()).isEqualTo(429);
    }

    private MockHttpServletResponse ejecutar(String uri, String ip) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", uri);
        req.setRemoteAddr(ip);
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filtro.doFilter(req, resp, new MockFilterChain());
        return resp;
    }
}