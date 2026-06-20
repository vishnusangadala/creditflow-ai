package com.creditflow.agent.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin HTTP gateway to the Python agent service. Keeping all knowledge of the
 * remote endpoint in one class means the rest of the backend depends on a Java
 * method, not on a URL.
 */
@Component
public class AgentServiceClient {

    private final RestClient agentRestClient;

    public AgentServiceClient(RestClient agentRestClient) {
        this.agentRestClient = agentRestClient;
    }

    public AgentRunResult run(AgentRunRequest request) {
        return agentRestClient.post()
                .uri("/run")
                .body(request)
                .retrieve()
                .body(AgentRunResult.class);
    }
}
