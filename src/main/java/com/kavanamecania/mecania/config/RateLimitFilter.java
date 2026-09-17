package com.kavanamecania.mecania.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limitador de peticiones por IP sobre `/api/**` (token bucket, en memoria).
 *
 * <p>Desactivable y separado del auth a propósito: un mismo contenedor basta para
 * frustrar fuerza bruta en el login y abuso de los endpoints caros (subida, chat)
 * sin montar Redis. En Render free la app es una sola instancia, así que un bucket
 * en memoria es suficiente; si mañana hay varias réplicas, esto pasa a un backend
 * compartido (Redis) sin cambiar el contrato del filtro.</p>
 *
 * <p>Solo actúa cuando {@code mecania.rate-limit.enabled=true} (perfil prod), para
 * que desarrollo y la suite de tests no topen contra un límite global por IP.</p>
 */
public class RateLimitFilter extends OncePerRequestFilter {

    static final String ERROR_CODE = "demasiadas_peticiones";
    static final String MENSAJE =
            "Demasiadas peticiones. Espera unos segundos e inténtalo de nuevo.";

    private final RateLimitProperties props;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties props) {
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !props.isEnabled() || !uri.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ruta = request.getRequestURI();
        boolean esAuth = ruta.startsWith("/api/auth/");
        int peticionesPorMinuto = esAuth
                ? props.getAuthPeticionesPorMinuto()
                : props.getApiPeticionesPorMinuto();

        String key = ip(request) + "|" + (esAuth ? "auth" : "api");
        TokenBucket bucket = buckets.computeIfAbsent(key,
                k -> new TokenBucket(peticionesPorMinuto, peticionesPorMinuto / 60.0));

        if (!bucket.tryAcquire()) {
            responder429(response, bucket.segundosParaUno());
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void responder429(HttpServletResponse response, long segundosRetryAfter)
            throws IOException {
        response.setStatus(429); // HTTP 429 Too Many Requests
        response.setHeader("Retry-After", String.valueOf(segundosRetryAfter));
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String cuerpo = "{\"error\":\"" + ERROR_CODE + "\",\"message\":\""
                + MENSAJE + "\"}";
        response.getWriter().write(cuerpo);
    }

    /**
     * Detrás de Render la IP real llega en X-Forwarded-For; sin ella (local) se
     * usa la IP de la conexión.
     */
    private String ip(HttpServletRequest request) {
        String fwd = request.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) {
            return fwd.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Token bucket simple con rellenado continuo. El reloj es del sistema: no hace
     * falta nada más para un límite de best-effort.
     */
    static final class TokenBucket {
        private final double capacidad;
        private final double tokensPorSegundo;
        private double tokens;
        private long ultima;

        TokenBucket(int capacidad, double tokensPorSegundo) {
            this.capacidad = Math.max(1, capacidad);
            this.tokensPorSegundo = Math.max(tokensPorSegundo, 0.0001);
            this.tokens = this.capacidad;
            this.ultima = System.nanoTime();
        }

        synchronized boolean tryAcquire() {
            rellenar();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        synchronized long segundosParaUno() {
            rellenar();
            if (tokens >= 1.0) {
                return 0;
            }
            return (long) Math.ceil(1.0 / tokensPorSegundo);
        }

        private void rellenar() {
            long ahora = System.nanoTime();
            double transcurrido = (ahora - ultima) / 1_000_000_000.0;
            tokens = Math.min(capacidad, tokens + transcurrido * tokensPorSegundo);
            ultima = ahora;
        }
    }
}