package model;

public record ComparisonSummary(
        int total,
        int regressions,
        int significantRegressions,
        int improvements,
        int significantImprovements,
        int noChange,
        int noBaseline,
        double worstRegressionPercent,
        double bestImprovementPercent
) {
    public boolean hasRegressions() {
        return regressions > 0;
    }

    public boolean hasSignificantRegressions() {
        return significantRegressions > 0;
    }
}