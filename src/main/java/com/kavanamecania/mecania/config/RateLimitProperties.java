package com.kavanamecania.mecania.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del rate limiting de `/api/**`.
 *
 * <p>Pensada para que esté APAGADA en desarrollo y tests (así la suite no choca
 * contra un límite global por IP) y ENCENDIDA solo en el perfil de producción.
 * Los valores reales están en `application-prod.properties`.</p>
 *
 * @param enabled            si el rate limiting está activo
 * @param apiPeticionesPorMinuto cuántas peticiones por minuto tolera cada IP sobre `/api/**`
 * @param authPeticionesPorMinuto cuántas tolera sobre `/api/auth/**` (login/registro: el punto
 *                              de fuerza bruta, así que va más bajo que el resto)
 */
// Se crea como @Bean en SecurityConfig (no @Component escaneado): asi los
// @WebMvcTest, que cargan SecurityConfig pero no escanean componentes, tienen
// el bean disponible sin romper su contexto.
@ConfigurationProperties(prefix = "mecania.rate-limit")
public class RateLimitProperties {

    private boolean enabled = false;
    private int apiPeticionesPorMinuto = 120;
    private int authPeticionesPorMinuto = 10;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getApiPeticionesPorMinuto() {
        return apiPeticionesPorMinuto;
    }

    public void setApiPeticionesPorMinuto(int apiPeticionesPorMinuto) {
        this.apiPeticionesPorMinuto = apiPeticionesPorMinuto;
    }

    public int getAuthPeticionesPorMinuto() {
        return authPeticionesPorMinuto;
    }

    public void setAuthPeticionesPorMinuto(int authPeticionesPorMinuto) {
        this.authPeticionesPorMinuto = authPeticionesPorMinuto;
    }
}