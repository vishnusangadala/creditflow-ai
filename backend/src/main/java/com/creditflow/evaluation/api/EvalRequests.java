package com.creditflow.evaluation.api;

import java.util.UUID;

public final class EvalRequests {

    private EvalRequests() {}

    public record CreateDatasetRequest(String name, String description) {}

    public record PromoteRequest(UUID workflowId, String caseName) {}
}
