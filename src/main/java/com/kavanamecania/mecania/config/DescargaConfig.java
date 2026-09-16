package com.kavanamecania.mecania.config;

import com.kavanamecania.mecania.domain.descarga.DescargadorUrl;
import com.kavanamecania.mecania.domain.descarga.ValidadorDestinoDescarga;
import com.kavanamecania.mecania.infrastructure.descarga.HttpDescargadorUrl;
import com.kavanamecania.mecania.infrastructure.descarga.ValidadorHostsPublicos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cableado del descargador de manuales remotos.
 *
 * <p>{@code mecania.descarga.permitir-hosts-privados=true} desactiva la
 * protección SSRF: solo para desarrollo con un servidor de ficheros local.
 * Por defecto está desactivada.</p>
 */
@Configuration
public class DescargaConfig {

    @Bean
    public ValidadorDestinoDescarga validadorDestinoDescarga(
            @Value("${mecania.descarga.permitir-hosts-privados:false}") boolean permitirHostsPrivados) {
        return permitirHostsPrivados ? ValidadorDestinoDescarga.permitirTodo() : new ValidadorHostsPublicos();
    }

    @Bean
    public DescargadorUrl descargadorUrl(
            ValidadorDestinoDescarga validadorDestinoDescarga,
            @Value("${mecania.descarga.max-bytes:10485760}") long maxBytes,
            @Value("${mecania.descarga.timeout-segundos:20}") int timeoutSegundos,
            @Value("${mecania.descarga.max-redirecciones:3}") int maxRedirecciones) {
        return new HttpDescargadorUrl(validadorDestinoDescarga, maxBytes, timeoutSegundos, maxRedirecciones);
    }
}
