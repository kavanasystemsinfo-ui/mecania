package com.kavanamecania.mecania.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación de la API con Swagger UI (springdoc).
 *
 * <p>La UI vive en {@code /swagger-ui.html} y el contrato en {@code /v3/api-docs}.
 * Configure the SecurityScheme para que, desde la propia UI, se pueda escribir
 * un token JWT y probar los endpoints. Está fuera de {@code /api/**}, así que no
 * exige autenticación para verse; los endpoints que describe sí la exigen.</p>
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Mecania API",
                version = "v1",
                description = "Asistente vehicular con RAG: vehículos, manuales, "
                        + "búsqueda asistida, chat sobre los manuales propios y alertas "
                        + "de mantenimiento. Toda la API exige un token JWT (Bearer) "
                        + "salvo /api/auth/**."))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {
}