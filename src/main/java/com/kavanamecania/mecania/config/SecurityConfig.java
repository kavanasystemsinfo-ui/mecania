package com.kavanamecania.mecania.config;

import com.kavanamecania.mecania.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Seguridad: JWT sin estado.
 *
 * <p>Con {@code mecania.auth.enabled=true} (por defecto), {@code /api/auth/**}
 * queda abierto y el resto de {@code /api/**} exige token. Con {@code false}
 * (perfil de test de lógica de negocio) todo queda abierto.</p>
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            @Value("${mecania.auth.enabled:true}") boolean authEnabled) throws Exception {
        http.csrf(csrf -> csrf.disable());

        // Cabeceras de seguridad. La más importante es la CSP: el token vive en
        // localStorage y, sin restringir los orígenes, cualquier XSS futuro lo
        // exfiltra sin esforzarse. La página carga Bootstrap desde jsdelivr y
        // usa script/estilos inline, así que la política no puede ser estricta:
        // se limita a los orígenes conocidos y bloquea el resto.
        http.headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
                        "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
                        "img-src 'self' data:; " +
                        "font-src 'self' https://cdn.jsdelivr.net; " +
                        "connect-src 'self'; " +
                        "object-src 'none'; " +
                        "frame-ancestors 'none'; " +
                        "base-uri 'self'"))
                .contentTypeOptions(withDefaults())
                .frameOptions(frame -> frame.deny()));

        if (authEnabled) {
            http.authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/auth/**").permitAll()
                            .requestMatchers("/api/**").authenticated()
                            .anyRequest().permitAll())
                    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                    .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                        response.setStatus(401);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write("{\"error\":\"no_autenticado\",\"message\":\"Token no válido o ausente\"}");
                    }));
        } else {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        }

        return http.build();
    }
}
