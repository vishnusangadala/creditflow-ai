package com.creditflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed view of the {@code creditflow.*} configuration namespace.
 * Beats sprinkling {@code @Value} strings across the codebase.
 */
@ConfigurationProperties(prefix = "creditflow")
public record CreditFlowProperties(
        AgentService agentService,
        Storage storage,
        Cors cors
) {
    public record AgentService(String baseUrl, int timeoutSeconds) {}

    public record Storage(String uploadDir) {}

    public record Cors(String allowedOrigins) {}
}
