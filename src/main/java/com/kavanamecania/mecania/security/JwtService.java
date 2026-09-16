package com.kavanamecania.mecania.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

/**
 * Genera y valida tokens JWT (HS256) con jjwt.
 *
 * <p>El secreto se lee de {@code mecania.jwt.secret} (debe tener al menos 32
 * bytes para HS256). En producción debe venir del entorno, nunca del código.</p>
 */
@Component
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${mecania.jwt.secret:}") String secret,
            @Value("${mecania.jwt.expiration-ms:86400000}") long expirationMs) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("mecania.jwt.secret no está configurada");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("mecania.jwt.secret debe tener al menos 32 bytes para HS256");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /** Genera un token con el id de usuario y su email. */
    public String generarToken(Long usuarioId, String email) {
        Date ahora = new Date();
        return Jwts.builder()
                .setSubject(email)
                .claim("uid", usuarioId)
                .setIssuedAt(ahora)
                .setExpiration(new Date(ahora.getTime() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Valida el token y devuelve la identidad. Vacío si es inválido, caducado o
     * está mal firmado.
     */
    public Optional<Identidad> validar(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            Number uid = claims.get("uid", Number.class);
            String email = claims.getSubject();
            if (uid == null || email == null) {
                return Optional.empty();
            }
            return Optional.of(new Identidad(uid.longValue(), email));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Identidad extraída de un token válido. */
    public record Identidad(Long usuarioId, String email) {
    }
}
