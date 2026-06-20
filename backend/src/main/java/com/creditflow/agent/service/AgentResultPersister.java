package com.creditflow.agent.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.creditflow.agent.client.AgentRunResult;
import com.creditflow.agent.domain.AgentOutput;
import com.creditflow.agent.domain.AgentRun;
import com.creditflow.agent.domain.AgentType;
import com.creditflow.agent.domain.CheckStatus;
import com.creditflow.agent.domain.RunStatus;
import com.creditflow.agent.domain.VerificationResult;
import com.creditflow.agent.repository.AgentOutputRepository;
import com.creditflow.agent.repository.AgentRunRepository;
import com.creditflow.agent.repository.VerificationResultRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Maps the agent service's {@link AgentRunResult} onto the five persistence
 * tables. Each agent telemetry becomes an {@code agent_runs} row; its artifact
 * becomes an {@code agent_outputs} row; the verifier's checks become
 * {@code verification_results} rows.
 *
 * <p>Pure mapping logic, isolated here so it can be unit-tested without Spring.
 */
@Service
public class AgentResultPersister {

    private final AgentRunRepository agentRunRepository;
    private final AgentOutputRepository agentOutputRepository;
    private final VerificationResultRepository verificationResultRepository;
    private final ObjectMapper objectMapper;

    public AgentResultPersister(AgentRunRepository agentRunRepository,
                                AgentOutputRepository agentOutputRepository,
                                VerificationResultRepository verificationResultRepository,
                                ObjectMapper objectMapper) {
        this.agentRunRepository = agentRunRepository;
        this.agentOutputRepository = agentOutputRepository;
        this.verificationResultRepository = verificationResultRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void persist(UUID workflowId, AgentRunResult result) {
        if (result.agentRuns() == null) {
            return;
        }
        for (AgentRunResult.AgentRunTelemetry telemetry : result.agentRuns()) {
            AgentType type = AgentType.valueOf(telemetry.agentType());

            AgentRun run = AgentRun.create(
                    workflowId,
                    type,
                    RunStatus.valueOf(telemetry.status()),
                    telemetry.model(),
                    telemetry.latencyMs(),
                    toJsonOrNull(telemetry.tokenUsage()),
                    telemetry.langsmithRunId(),
                    telemetry.errorMessage()
            );
            agentRunRepository.save(run);

            JsonNode output = outputFor(type, result);
            if (output != null && !output.isNull()) {
                agentOutputRepository.save(AgentOutput.create(run.getId(), output.toString()));
            }

            if (type == AgentType.VERIFIER && result.verification() != null
                    && result.verification().checks() != null) {
                for (AgentRunResult.Check check : result.verification().checks()) {
                    verificationResultRepository.save(VerificationResult.create(
                            workflowId,
                            run.getId(),
                            check.checkName(),
                            check.category(),
                            check.target(),
                            CheckStatus.valueOf(check.status()),
                            check.expected(),
                            check.actual(),
                            check.evidence()
                    ));
                }
            }
        }
    }

    /** Pick the structured artifact a given agent produced. */
    JsonNode outputFor(AgentType type, AgentRunResult result) {
        return switch (type) {
            case EXTRACTION -> result.extraction();
            case FINANCIAL_ANALYSIS -> result.metrics();
            case RISK_ASSESSMENT -> result.risk();
            case CREDIT_MEMO -> result.memo();
            case VERIFIER -> result.verification() == null
                    ? null
                    : objectMapper.valueToTree(result.verification());
        };
    }

    private String toJsonOrNull(Map<String, Integer> tokenUsage) {
        if (tokenUsage == null || tokenUsage.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(tokenUsage);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
