package com.kavanamecania.mecania.application;

import com.kavanamecania.mecania.application.dto.AuthResponse;
import com.kavanamecania.mecania.application.dto.LoginRequest;
import com.kavanamecania.mecania.application.dto.RegisterRequest;
import com.kavanamecania.mecania.domain.exception.CredencialesInvalidasException;
import com.kavanamecania.mecania.domain.exception.UsuarioYaExisteException;
import com.kavanamecania.mecania.domain.model.Usuario;
import com.kavanamecania.mecania.domain.repository.UsuarioRepository;
import com.kavanamecania.mecania.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Registro e inicio de sesión de usuarios.
 */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse registrar(RegisterRequest req) {
        if (usuarioRepository.findByEmail(req.email()).isPresent()) {
            throw new UsuarioYaExisteException(req.email());
        }
        Usuario usuario = Usuario.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .createdAt(Instant.now())
                .build();
        Usuario guardado = usuarioRepository.save(usuario);
        String token = jwtService.generarToken(guardado.getId(), guardado.getEmail());
        return new AuthResponse(token, guardado.getEmail());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        Usuario usuario = usuarioRepository.findByEmail(req.email())
                .orElseThrow(CredencialesInvalidasException::new);
        if (!passwordEncoder.matches(req.password(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }
        String token = jwtService.generarToken(usuario.getId(), usuario.getEmail());
        return new AuthResponse(token, usuario.getEmail());
    }
}
