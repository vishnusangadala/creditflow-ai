package com.creditflow.agent.client;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/** Request body for the agent service's {@code /run} endpoint. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AgentRunRequest(String workflowId, List<InputDocument> documents) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record InputDocument(String filename, String contentBase64) {}
}
