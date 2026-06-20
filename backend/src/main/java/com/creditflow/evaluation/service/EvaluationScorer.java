package com.creditflow.evaluation.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

/**
 * Pure, deterministic scoring of an agent's outputs against a golden answer.
 * No LLM, no I/O — fully unit-testable.
 *
 * <p>Two of the dimensions measure the trust machinery itself:
 * <ul>
 *   <li><b>verifier_recall</b> — of the fields a human had to correct (i.e. the
 *       agent got wrong), how many did the Verifier flag? High recall = the
 *       safety net catches real errors.</li>
 *   <li><b>verifier_precision</b> — of the fields the Verifier flagged, how many
 *       did a human actually correct? Low precision = noisy false alarms.</li>
 * </ul>
 */
@Service
public class EvaluationScorer {

    private static final double METRIC_TOLERANCE = 0.01;

    public record ScoreInput(
            Map<String, String> expectedExtraction,
            Map<String, String> actualExtraction,
            Map<String, Double> expectedMetrics,
            Map<String, Double> actualMetrics,
            String expectedRisk,
            String actualRisk,
            Set<String> humanCorrectedTargets,
            Set<String> verifierFlaggedTargets
    ) {}

    public record DimensionScore(String dimension, double score, String detail) {}

    public record CaseScore(double overall, List<DimensionScore> dimensions) {}

    public CaseScore score(ScoreInput in) {
        List<DimensionScore> dims = new ArrayList<>();

        if (in.expectedExtraction() != null && !in.expectedExtraction().isEmpty()) {
            dims.add(extractionAccuracy(in.expectedExtraction(), in.actualExtraction()));
        }
        if (in.expectedMetrics() != null && !in.expectedMetrics().isEmpty()) {
            dims.add(metricAccuracy(in.expectedMetrics(), in.actualMetrics()));
        }
        if (in.expectedRisk() != null) {
            dims.add(riskAgreement(in.expectedRisk(), in.actualRisk()));
        }
        if (in.humanCorrectedTargets() != null && !in.humanCorrectedTargets().isEmpty()) {
            dims.add(verifierRecall(in.humanCorrectedTargets(), safe(in.verifierFlaggedTargets())));
        }
        if (in.verifierFlaggedTargets() != null && !in.verifierFlaggedTargets().isEmpty()) {
            dims.add(verifierPrecision(safe(in.humanCorrectedTargets()), in.verifierFlaggedTargets()));
        }

        double overall = dims.isEmpty() ? 0.0
                : round(dims.stream().mapToDouble(DimensionScore::score).average().orElse(0.0));
        return new CaseScore(overall, dims);
    }

    private DimensionScore extractionAccuracy(Map<String, String> expected, Map<String, String> actual) {
        Map<String, String> act = safe(actual);
        int matches = 0;
        for (var e : expected.entrySet()) {
            if (normalize(act.get(e.getKey())).equals(normalize(e.getValue()))) {
                matches++;
            }
        }
        double score = round((double) matches / expected.size());
        return new DimensionScore("extraction_accuracy", score, matches + "/" + expected.size() + " fields match");
    }

    private DimensionScore metricAccuracy(Map<String, Double> expected, Map<String, Double> actual) {
        Map<String, Double> act = safe(actual);
        int matches = 0;
        for (var e : expected.entrySet()) {
            Double a = act.get(e.getKey());
            if (a != null && e.getValue() != null && Math.abs(a - e.getValue()) <= METRIC_TOLERANCE) {
                matches++;
            }
        }
        double score = round((double) matches / expected.size());
        return new DimensionScore("metric_accuracy", score, matches + "/" + expected.size() + " metrics match");
    }

    private DimensionScore riskAgreement(String expected, String actual) {
        boolean agree = expected.equalsIgnoreCase(actual == null ? "" : actual);
        return new DimensionScore("risk_agreement", agree ? 1.0 : 0.0,
                "expected=" + expected + ", actual=" + actual);
    }

    private DimensionScore verifierRecall(Set<String> corrected, Set<String> flagged) {
        long caught = corrected.stream().filter(flagged::contains).count();
        double score = round((double) caught / corrected.size());
        return new DimensionScore("verifier_recall", score,
                caught + "/" + corrected.size() + " human-corrected fields were flagged by the verifier");
    }

    private DimensionScore verifierPrecision(Set<String> corrected, Set<String> flagged) {
        long real = flagged.stream().filter(corrected::contains).count();
        double score = round((double) real / flagged.size());
        return new DimensionScore("verifier_precision", score,
                real + "/" + flagged.size() + " verifier flags matched a human correction");
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private static <K, V> Map<K, V> safe(Map<K, V> m) {
        return m == null ? Map.of() : m;
    }

    private static <T> Set<T> safe(Set<T> s) {
        return s == null ? Set.of() : s;
    }

    private static double round(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
