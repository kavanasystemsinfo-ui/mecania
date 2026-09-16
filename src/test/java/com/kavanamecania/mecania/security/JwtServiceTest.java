package com.kavanamecania.mecania.security;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-0123456789abcdef0123456789abcdef";

    @Test
    void token_valido_devuelve_la_identidad() {
        JwtService service = new JwtService(SECRET, 60_000);
        String token = service.generarToken(42L, "a@b.com");

        Optional<JwtService.Identidad> identidad = service.validar(token);

        assertThat(identidad).isPresent();
        assertThat(identidad.get().usuarioId()).isEqualTo(42L);
        assertThat(identidad.get().email()).isEqualTo("a@b.com");
    }

    @Test
    void token_manipulado_es_invalido() {
        JwtService service = new JwtService(SECRET, 60_000);
        String token = service.generarToken(42L, "a@b.com");
        String manipulado = token.substring(0, token.length() - 3) + "abc";

        assertThat(service.validar(manipulado)).isEmpty();
    }

    @Test
    void token_firmado_con_otro_secreto_es_invalido() {
        JwtService service = new JwtService(SECRET, 60_000);
        JwtService otro = new JwtService("otro-secreto-0123456789abcdef0123456789abcdef", 60_000);

        String token = otro.generarToken(1L, "a@b.com");

        assertThat(service.validar(token)).isEmpty();
    }

    @Test
    void token_caducado_es_invalido() {
        // expiration negativo: el token ya nace caducado, determinista sin sleep.
        JwtService service = new JwtService(SECRET, -1_000);
        String token = service.generarToken(42L, "a@b.com");

        assertThat(service.validar(token)).isEmpty();
    }

    @Test
    void secreto_demasiado_corto_se_rechaza() {
        assertThatThrownBy(() -> new JwtService("corto", 60_000))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void secreto_vacio_se_rechaza() {
        assertThatThrownBy(() -> new JwtService("", 60_000))
                .isInstanceOf(IllegalStateException.class);
    }
}
