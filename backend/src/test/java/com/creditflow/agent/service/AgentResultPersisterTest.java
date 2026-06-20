package com.creditflow.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.creditflow.agent.client.AgentRunResult;
import com.creditflow.agent.domain.AgentOutput;
import com.creditflow.agent.domain.AgentRun;
import com.creditflow.agent.domain.AgentType;
import com.creditflow.agent.domain.VerificationResult;
import com.creditflow.agent.repository.AgentOutputRepository;
import com.creditflow.agent.repository.AgentRunRepository;
import com.creditflow.agent.repository.VerificationResultRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit test for the response -> entities mapping. No Spring context, no DB:
 * pure logic, fast and deterministic.
 */
class AgentResultPersisterTest {

    private AgentRunRepository runRepo;
    private AgentOutputRepository outputRepo;
    private VerificationResultRepository verificationRepo;
    private AgentResultPersister persister;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        runRepo = Mockito.mock(AgentRunRepository.class);
        outputRepo = Mockito.mock(AgentOutputRepository.class);
        verificationRepo = Mockito.mock(VerificationResultRepository.class);
        persister = new AgentResultPersister(runRepo, outputRepo, verificationRepo, mapper);
    }

    @Test
    void persistsRunsOutputsAndVerificationChecks() throws Exception {
        UUID workflowId = UUID.randomUUID();

        var extractionNode = mapper.readTree("{\"borrower\":{\"value\":\"Acme\"}}");
        var extractionRun = new AgentRunResult.AgentRunTelemetry(
                "EXTRACTION", "SUCCESS", "gpt-4o-mini", 1200,
                Map.of("total_tokens", 500), "run-1", null);
        var verifierRun = new AgentRunResult.AgentRunTelemetry(
                "VERIFIER", "SUCCESS", "gpt-4o-mini", 800, Map.of(), "run-2", null);

        var checks = List.of(
                new AgentRunResult.Check("grounding:borrower", "GROUNDING", "borrower",
                        "PASS", "present", "ratio 1.0", "Acme Corp"),
                new AgentRunResult.Check("recompute:debt_to_ebitda", "RECOMPUTATION",
                        "debt_to_ebitda", "FAIL", "2.5", "reported=3.0", "total_debt/ebitda"));
        var verification = new AgentRunResult.Verification("FAIL", checks);

        var result = new AgentRunResult(
                workflowId.toString(), "NEEDS_REVIEW", "Acme", "MODERATE",
                List.of(), List.of(extractionRun, verifierRun),
                extractionNode, null, null, null, verification, null);

        persister.persist(workflowId, result);

        // two runs persisted
        ArgumentCaptor<AgentRun> runCaptor = ArgumentCaptor.forClass(AgentRun.class);
        verify(runRepo, times(2)).save(runCaptor.capture());
        assertThat(runCaptor.getAllValues())
                .extracting(AgentRun::getAgentType)
                .containsExactly(AgentType.EXTRACTION, AgentType.VERIFIER);

        // extraction output + verifier output both stored
        ArgumentCaptor<AgentOutput> outputCaptor = ArgumentCaptor.forClass(AgentOutput.class);
        verify(outputRepo, times(2)).save(outputCaptor.capture());
        assertThat(outputCaptor.getAllValues().get(0).getOutput()).contains("Acme");

        // both verification checks persisted
        ArgumentCaptor<VerificationResult> checkCaptor = ArgumentCaptor.forClass(VerificationResult.class);
        verify(verificationRepo, times(2)).save(checkCaptor.capture());
        assertThat(checkCaptor.getAllValues())
                .extracting(VerificationResult::getCheckName)
                .containsExactly("grounding:borrower", "recompute:debt_to_ebitda");
    }

    @Test
    void outputForMapsEachAgentToItsArtifact() throws Exception {
        var metricsNode = mapper.readTree("{\"debt_to_ebitda\":{\"value\":2.5}}");
        var result = new AgentRunResult(
                "w", "COMPLETED", null, null, List.of(), List.of(),
                null, metricsNode, null, null, null, null);

        assertThat(persister.outputFor(AgentType.FINANCIAL_ANALYSIS, result)).isEqualTo(metricsNode);
        assertThat(persister.outputFor(AgentType.EXTRACTION, result)).isNull();
    }
}
