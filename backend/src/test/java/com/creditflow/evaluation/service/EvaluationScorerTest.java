package com.creditflow.evaluation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.creditflow.evaluation.service.EvaluationScorer.CaseScore;
import com.creditflow.evaluation.service.EvaluationScorer.DimensionScore;
import com.creditflow.evaluation.service.EvaluationScorer.ScoreInput;

/** Pure unit tests for the deterministic eval scorer — no Spring, no DB. */
class EvaluationScorerTest {

    private final EvaluationScorer scorer = new EvaluationScorer();

    private double dim(CaseScore s, String name) {
        return s.dimensions().stream()
                .filter(d -> d.dimension().equals(name))
                .map(DimensionScore::score)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void perfectCaseScoresOne() {
        CaseScore s = scorer.score(new ScoreInput(
                Map.of("borrower", "Acme", "interest_rate", "7.5%"),
                Map.of("borrower", "Acme", "interest_rate", "7.5%"),
                Map.of("debt_to_ebitda", 2.5),
                Map.of("debt_to_ebitda", 2.5),
                "MODERATE", "MODERATE",
                Set.of(), Set.of()));
        assertThat(s.overall()).isEqualTo(1.0);
    }

    @Test
    void extractionAccuracyIsPartialWhenSomeFieldsWrong() {
        CaseScore s = scorer.score(new ScoreInput(
                Map.of("borrower", "Acme", "interest_rate", "7.5%", "collateral", "Real estate"),
                Map.of("borrower", "Acme", "interest_rate", "9.9%", "collateral", "Real estate"),
                Map.of(), Map.of(), null, null, Set.of(), Set.of()));
        assertThat(dim(s, "extraction_accuracy")).isCloseTo(0.6667, within(0.001));
    }

    @Test
    void verifierRecallMeasuresWhetherFlagsCoverHumanCorrections() {
        // Human corrected two fields; verifier flagged one of them (plus an extra).
        CaseScore s = scorer.score(new ScoreInput(
                Map.of(), Map.of(), Map.of(), Map.of(), null, null,
                Set.of("interest_rate", "financials.ebitda"),       // human-corrected (truly wrong)
                Set.of("interest_rate", "borrower")));               // verifier flags
        assertThat(dim(s, "verifier_recall")).isEqualTo(0.5);       // 1 of 2 corrected fields flagged
        assertThat(dim(s, "verifier_precision")).isEqualTo(0.5);    // 1 of 2 flags were real
    }

    @Test
    void riskDisagreementScoresZeroOnThatDimension() {
        CaseScore s = scorer.score(new ScoreInput(
                Map.of(), Map.of(), Map.of(), Map.of(), "HIGH", "LOW", Set.of(), Set.of()));
        assertThat(dim(s, "risk_agreement")).isEqualTo(0.0);
    }

    @Test
    void dimensionsWithNoExpectedDataAreSkipped() {
        CaseScore s = scorer.score(new ScoreInput(
                Map.of("borrower", "Acme"), Map.of("borrower", "Acme"),
                null, null, null, null, null, null));
        List<String> dims = s.dimensions().stream().map(DimensionScore::dimension).toList();
        assertThat(dims).containsExactly("extraction_accuracy");
    }
}
