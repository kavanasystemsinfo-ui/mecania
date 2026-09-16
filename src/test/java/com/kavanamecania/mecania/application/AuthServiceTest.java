package com.kavanamecania.mecania.application;

import com.kavanamecania.mecania.application.dto.AuthResponse;
import com.kavanamecania.mecania.application.dto.LoginRequest;
import com.kavanamecania.mecania.application.dto.RegisterRequest;
import com.kavanamecania.mecania.domain.exception.CredencialesInvalidasException;
import com.kavanamecania.mecania.domain.exception.UsuarioYaExisteException;
import com.kavanamecania.mecania.domain.model.Usuario;
import com.kavanamecania.mecania.domain.repository.UsuarioRepository;
import com.kavanamecania.mecania.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthService authService;

    @Test
    void registrar_hashea_la_password_y_devuelve_token() {
        when(usuarioRepository.findByEmail("nuevo@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secreto123")).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtService.generarToken(1L, "nuevo@test.com")).thenReturn("token123");

        AuthResponse r = authService.registrar(new RegisterRequest("nuevo@test.com", "secreto123"));

        assertThat(r.token()).isEqualTo("token123");
        assertThat(r.email()).isEqualTo("nuevo@test.com");
    }

    @Test
    void registrar_email_duplicado_lanza_excepcion() {
        when(usuarioRepository.findByEmail("dup@test.com")).thenReturn(Optional.of(new Usuario()));

        assertThatThrownBy(() -> authService.registrar(new RegisterRequest("dup@test.com", "secreto123")))
                .isInstanceOf(UsuarioYaExisteException.class);
    }

    @Test
    void login_correcto_devuelve_token() {
        Usuario u = Usuario.builder().id(7L).email("ok@test.com").passwordHash("hash").build();
        when(usuarioRepository.findByEmail("ok@test.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("secreto123", "hash")).thenReturn(true);
        when(jwtService.generarToken(7L, "ok@test.com")).thenReturn("token7");

        AuthResponse r = authService.login(new LoginRequest("ok@test.com", "secreto123"));

        assertThat(r.token()).isEqualTo("token7");
    }

    @Test
    void login_password_incorrecta_lanza_excepcion() {
        Usuario u = Usuario.builder().id(7L).email("ok@test.com").passwordHash("hash").build();
        when(usuarioRepository.findByEmail("ok@test.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("mala", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ok@test.com", "mala")))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void login_email_inexistente_lanza_excepcion() {
        when(usuarioRepository.findByEmail("no@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("no@test.com", "secreto123")))
                .isInstanceOf(CredencialesInvalidasException.class);
    }
}
