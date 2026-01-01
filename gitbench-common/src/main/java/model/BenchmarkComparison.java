package model;

import enums.ChangeType;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class BenchmarkComparison implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private MethodSignature methodSignature;

    private BenchmarkResult currentResult;

    private BenchmarkResult baselineResult;

    private double percentageChange;

    private double absoluteChange;

    private ChangeType changeType;

    private boolean statisticallySignificant;

    private double pValue;

    private double confidenceLevel;

    public static ChangeType calculateChangeType(double percentageChange) {
        if (Double.isNaN(percentageChange)) {
            return ChangeType.NO_BASELINE;
        }
        if (percentageChange > 10.0) {
            return ChangeType.SIGNIFICANT_REGRESSION;
        } else if (percentageChange > 5.0) {
            return ChangeType.MINOR_REGRESSION;
        } else if (percentageChange < -10.0) {
            return ChangeType.SIGNIFICANT_IMPROVEMENT;
        } else if (percentageChange < -5.0) {
            return ChangeType.MINOR_IMPROVEMENT;
        } else {
            return ChangeType.NO_CHANGE;
        }
    }

    public String getSummary() {
        if (baselineResult == null) {
            return String.format("%s: %.2f ms (no baseline)",
                    methodSignature.getShortName(),
                    currentResult.getExecutionStats().getAverageTime());
        }

        String changeSymbol = percentageChange >= 0 ? "+" : "";
        String changeLabel = switch (changeType) {
            case SIGNIFICANT_REGRESSION -> "(regression)";
            case MINOR_REGRESSION -> "(minor regression)";
            case SIGNIFICANT_IMPROVEMENT -> "(improvement)";
            case MINOR_IMPROVEMENT -> "(minor improvement)";
            case NO_CHANGE -> "(no change)";
            case NO_BASELINE -> "(no baseline)";
        };

        return String.format("%s: %.2f ms → %.2f ms (%s%.1f%%) %s",
                methodSignature.getShortName(),
                baselineResult.getExecutionStats().getAverageTime(),
                currentResult.getExecutionStats().getAverageTime(),
                changeSymbol,
                percentageChange,
                changeLabel);
    }

    public boolean isRegression() {
        return changeType == ChangeType.SIGNIFICANT_REGRESSION ||
                changeType == ChangeType.MINOR_REGRESSION;
    }

    public boolean isImprovement() {
        return changeType == ChangeType.SIGNIFICANT_IMPROVEMENT ||
                changeType == ChangeType.MINOR_IMPROVEMENT;
    }
}
