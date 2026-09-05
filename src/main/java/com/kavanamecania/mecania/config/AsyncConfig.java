package com.kavanamecania.mecania.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Habilita @Async a nivel de aplicación y configura el pool dedicado para procesamiento de documentos.
 * El pool es separado del de Spring MVC para que las tareas largas no bloqueen el servidor web.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "documentoProcessorExecutor")
    public Executor documentoProcessorExecutor(
            @Value("${mecania.procesamiento.pool-size:2}") int corePoolSize,
            @Value("${mecania.procesamiento.max-pool-size:4}") int maxPoolSize,
            @Value("${mecania.procesamiento.queue-capacity:100}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("mecania-proc-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}