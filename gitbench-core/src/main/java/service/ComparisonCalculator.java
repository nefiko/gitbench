package service;


import enums.ChangeType;
import model.BenchmarkComparison;
import model.BenchmarkResult;
import model.ComparisonSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ComparisonCalculator {

    private static final double DEFAULT_CONFIDENCE_LEVEL = 0.95;

    public List<BenchmarkComparison> compare(List<BenchmarkResult> currentResults,
                                             List<BenchmarkResult> baselineResults) {
        Map<String, BenchmarkResult> baselineMap = baselineResults.stream()
                .filter(BenchmarkResult::isSuccessful)
                .collect(Collectors.toMap(
                        r -> r.getMethodSignature().getUniqueId(),
                        Function.identity(),
                        (a, b) -> b
                ));

        List<BenchmarkComparison> comparisons = new ArrayList<>();

        for (BenchmarkResult current : currentResults) {
            if (!current.isSuccessful()) {
                continue;
            }

            String signatureId = current.getMethodSignature().getUniqueId();
            BenchmarkResult baseline = baselineMap.get(signatureId);

            comparisons.add(compare(current, baseline));
        }

        return comparisons;
    }

    public BenchmarkComparison compare(BenchmarkResult current, BenchmarkResult baseline) {
        if (baseline == null) {
            return BenchmarkComparison.builder()
                    .methodSignature(current.getMethodSignature())
                    .currentResult(current)
                    .baselineResult(null)
                    .percentageChange(Double.NaN)
                    .absoluteChange(0.0)
                    .changeType(ChangeType.NO_BASELINE)
                    .confidenceLevel(DEFAULT_CONFIDENCE_LEVEL)
                    .build();
        }

        double currentTime = current.getExecutionStats().getAverageTime();
        double baselineTime = baseline.getExecutionStats().getAverageTime();

        double absoluteChange = currentTime - baselineTime;
        double percentageChange = ((currentTime - baselineTime) / baselineTime) * 100.0;

        ChangeType changeType = BenchmarkComparison.calculateChangeType(percentageChange);
        boolean significant = isStatisticallySignificant(current, baseline);

        return BenchmarkComparison.builder()
                .methodSignature(current.getMethodSignature())
                .currentResult(current)
                .baselineResult(baseline)
                .percentageChange(percentageChange)
                .absoluteChange(absoluteChange)
                .changeType(changeType)
                .statisticallySignificant(significant)
                .confidenceLevel(DEFAULT_CONFIDENCE_LEVEL)
                .build();
    }

    private boolean isStatisticallySignificant(BenchmarkResult current, BenchmarkResult baseline) {
        var currentStats = current.getExecutionStats();
        var baselineStats = baseline.getExecutionStats();

        if (currentStats.getConfidenceInterval() == null ||
                baselineStats.getConfidenceInterval() == null) {
            double currentMean = currentStats.getAverageTime();
            double baselineMean = baselineStats.getAverageTime();
            double currentStd = currentStats.getStandardDeviation();
            double baselineStd = baselineStats.getStandardDeviation();

            double combinedStd = Math.sqrt(currentStd * currentStd + baselineStd * baselineStd);
            return Math.abs(currentMean - baselineMean) > 2 * combinedStd;
        }

        double[] currentCI = currentStats.getConfidenceInterval();
        double[] baselineCI = baselineStats.getConfidenceInterval();

        boolean overlaps = currentCI[0] <= baselineCI[1] && baselineCI[0] <= currentCI[1];
        return !overlaps;
    }

    public ComparisonSummary summarize(List<BenchmarkComparison> comparisons) {
        int total = comparisons.size();
        int regressions = 0;
        int significantRegressions = 0;
        int improvements = 0;
        int significantImprovements = 0;
        int noChange = 0;
        int noBaseline = 0;
        double worstRegression = 0.0;
        double bestImprovement = 0.0;

        for (BenchmarkComparison comp : comparisons) {
            switch (comp.getChangeType()) {
                case SIGNIFICANT_REGRESSION -> {
                    regressions++;
                    significantRegressions++;
                    worstRegression = Math.max(worstRegression, comp.getPercentageChange());
                }
                case MINOR_REGRESSION -> {
                    regressions++;
                    worstRegression = Math.max(worstRegression, comp.getPercentageChange());
                }
                case SIGNIFICANT_IMPROVEMENT -> {
                    improvements++;
                    significantImprovements++;
                    bestImprovement = Math.min(bestImprovement, comp.getPercentageChange());
                }
                case MINOR_IMPROVEMENT -> {
                    improvements++;
                    bestImprovement = Math.min(bestImprovement, comp.getPercentageChange());
                }
                case NO_CHANGE -> noChange++;
                case NO_BASELINE -> noBaseline++;
            }
        }

        return new ComparisonSummary(
                total, regressions, significantRegressions,
                improvements, significantImprovements,
                noChange, noBaseline,
                worstRegression, bestImprovement
        );
    }
}