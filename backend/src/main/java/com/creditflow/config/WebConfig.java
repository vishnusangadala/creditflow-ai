package com.creditflow.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC configuration. Enables {@link CreditFlowProperties} and opens CORS for the
 * React dev server (origin is configurable so prod can lock it down).
 */
@Configuration
@EnableConfigurationProperties(CreditFlowProperties.class)
public class WebConfig implements WebMvcConfigurer {

    private final CreditFlowProperties properties;

    public WebConfig(CreditFlowProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(properties.cors().allowedOrigins().split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
