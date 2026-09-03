package com.kavanamecania.mecania.application;

import com.kavanamecania.mecania.domain.model.Usuario;
import com.kavanamecania.mecania.domain.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario save(Usuario usuario) {
        String hashedPassword = passwordEncoder.encode(usuario.getPasswordHash());
        usuario.setPasswordHash(hashedPassword);
        return usuarioRepository.save(usuario);
    }

    public Optional<Usuario> findByEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    public Optional<Usuario> findById(Long id) {
        return usuarioRepository.findById(id);
    }
}