package com.creditflow.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Builds the {@link RestClient} used to call the Python agent service.
 *
 * <p>The read timeout is generous on purpose: the {@code /run} call executes the
 * full five-agent LLM pipeline synchronously and can take a couple of minutes on
 * large documents. The connect timeout stays short to fail fast when the service
 * is down.
 */
@Configuration
public class AgentClientConfig {

    @Bean
    RestClient agentRestClient(CreditFlowProperties properties) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(properties.agentService().timeoutSeconds()));

        return RestClient.builder()
                .baseUrl(properties.agentService().baseUrl())
                .requestFactory(factory)
                .build();
    }
}
