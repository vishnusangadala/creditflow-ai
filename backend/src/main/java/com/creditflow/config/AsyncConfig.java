package com.creditflow.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Dedicated thread pool for running workflows off the request thread.
 *
 * <p>Phase 1 keeps this intentionally simple — a bounded pool, no external
 * broker. A real job queue (Kafka/SQS/Temporal) is a Phase 2 concern; building
 * it now would be premature.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "workflowExecutor")
    public Executor workflowExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("workflow-");
        executor.initialize();
        return executor;
    }
}
