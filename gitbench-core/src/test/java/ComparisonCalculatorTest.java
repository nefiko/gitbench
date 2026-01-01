import enums.ChangeType;
import model.BenchmarkComparison;
import model.BenchmarkResult;
import model.ComparisonSummary;
import model.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.ComparisonCalculator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ComparisonCalculatorTest {

    ComparisonCalculator calculator;

    @BeforeEach
    void setup() {
        calculator = new ComparisonCalculator();
    }

    @Test
    void compare_detectsSignificantRegression() {
        BenchmarkResult current = createResult("Method#test()", 120.0);
        BenchmarkResult baseline = createResult("Method#test()", 100.0);

        BenchmarkComparison result = calculator.compare(current, baseline);

        assertThat(result.getChangeType()).isEqualTo(ChangeType.SIGNIFICANT_REGRESSION);
        assertThat(result.getPercentageChange()).isEqualTo(20.0);
        assertThat(result.isRegression()).isTrue();
    }

    @Test
    void compare_detectsMinorRegression() {
        BenchmarkResult current = createResult("Method#test()", 107.0);
        BenchmarkResult baseline = createResult("Method#test()", 100.0);

        BenchmarkComparison result = calculator.compare(current, baseline);

        assertThat(result.getChangeType()).isEqualTo(ChangeType.MINOR_REGRESSION);
        assertThat(result.isRegression()).isTrue();
    }

    @Test
    void compare_detectsSignificantImprovement() {
        BenchmarkResult current = createResult("Method#test()", 80.0);
        BenchmarkResult baseline = createResult("Method#test()", 100.0);

        BenchmarkComparison result = calculator.compare(current, baseline);

        assertThat(result.getChangeType()).isEqualTo(ChangeType.SIGNIFICANT_IMPROVEMENT);
        assertThat(result.getPercentageChange()).isEqualTo(-20.0);
        assertThat(result.isImprovement()).isTrue();
    }

    @Test
    void compare_detectsMinorImprovement() {
        BenchmarkResult current = createResult("Method#test()", 93.0);
        BenchmarkResult baseline = createResult("Method#test()", 100.0);

        BenchmarkComparison result = calculator.compare(current, baseline);

        assertThat(result.getChangeType()).isEqualTo(ChangeType.MINOR_IMPROVEMENT);
        assertThat(result.isImprovement()).isTrue();
    }

    @Test
    void compare_detectsNoChange() {
        BenchmarkResult current = createResult("Method#test()", 102.0);
        BenchmarkResult baseline = createResult("Method#test()", 100.0);

        BenchmarkComparison result = calculator.compare(current, baseline);

        assertThat(result.getChangeType()).isEqualTo(ChangeType.NO_CHANGE);
        assertThat(result.isRegression()).isFalse();
        assertThat(result.isImprovement()).isFalse();
    }

    @Test
    void compare_handlesNoBaseline() {
        BenchmarkResult current = createResult("Method#test()", 100.0);

        BenchmarkComparison result = calculator.compare(current, null);

        assertThat(result.getChangeType()).isEqualTo(ChangeType.NO_BASELINE);
        assertThat(result.getBaselineResult()).isNull();
        assertThat(Double.isNaN(result.getPercentageChange())).isTrue();
    }

    @Test
    void compare_calculatesAbsoluteChange() {
        BenchmarkResult current = createResult("Method#test()", 150.0);
        BenchmarkResult baseline = createResult("Method#test()", 100.0);

        BenchmarkComparison result = calculator.compare(current, baseline);

        assertThat(result.getAbsoluteChange()).isEqualTo(50.0);
    }

    @Test
    void compareList_matchesByMethodSignature() {
        BenchmarkResult currentA = createResult("ClassA#methodA()", 110.0);
        BenchmarkResult currentB = createResult("ClassB#methodB()", 90.0);
        BenchmarkResult baselineA = createResult("ClassA#methodA()", 100.0);
        BenchmarkResult baselineB = createResult("ClassB#methodB()", 100.0);

        List<BenchmarkComparison> results = calculator.compare(
                List.of(currentA, currentB),
                List.of(baselineA, baselineB)
        );

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getMethodSignature().getUniqueId()).isEqualTo("ClassA#methodA()");
        assertThat(results.get(1).getMethodSignature().getUniqueId()).isEqualTo("ClassB#methodB()");
    }

    @Test
    void compareList_skipsFailedResults() {
        BenchmarkResult successful = createResult("Method#success()", 100.0);
        BenchmarkResult failed = createResult("Method#failed()", 100.0);
        failed.setSuccessful(false);

        List<BenchmarkComparison> results = calculator.compare(
                List.of(successful, failed),
                List.of()
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMethodSignature().getUniqueId()).isEqualTo("Method#success()");
    }

    @Test
    void compareList_handlesNewMethodsWithoutBaseline() {
        BenchmarkResult newMethod = createResult("NewClass#newMethod()", 100.0);

        List<BenchmarkComparison> results = calculator.compare(
                List.of(newMethod),
                List.of()
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getChangeType()).isEqualTo(ChangeType.NO_BASELINE);
    }

    @Test
    void summarize_countsRegressions() {
        List<BenchmarkComparison> comparisons = List.of(
                createComparison(ChangeType.SIGNIFICANT_REGRESSION),
                createComparison(ChangeType.MINOR_REGRESSION),
                createComparison(ChangeType.NO_CHANGE)
        );

        ComparisonSummary summary = calculator.summarize(comparisons);

        assertThat(summary.regressions()).isEqualTo(2);
        assertThat(summary.significantRegressions()).isEqualTo(1);
        assertThat(summary.hasRegressions()).isTrue();
        assertThat(summary.hasSignificantRegressions()).isTrue();
    }

    @Test
    void summarize_countsImprovements() {
        List<BenchmarkComparison> comparisons = List.of(
                createComparison(ChangeType.SIGNIFICANT_IMPROVEMENT),
                createComparison(ChangeType.MINOR_IMPROVEMENT),
                createComparison(ChangeType.NO_CHANGE)
        );

        ComparisonSummary summary = calculator.summarize(comparisons);

        assertThat(summary.improvements()).isEqualTo(2);
        assertThat(summary.significantImprovements()).isEqualTo(1);
    }

    @Test
    void summarize_tracksWorstRegression() {
        BenchmarkComparison worst = createComparison(ChangeType.SIGNIFICANT_REGRESSION);
        worst.setPercentageChange(25.0);
        BenchmarkComparison minor = createComparison(ChangeType.MINOR_REGRESSION);
        minor.setPercentageChange(7.0);

        ComparisonSummary summary = calculator.summarize(List.of(worst, minor));

        assertThat(summary.worstRegressionPercent()).isEqualTo(25.0);
    }

    @Test
    void summarize_tracksBestImprovement() {
        BenchmarkComparison best = createComparison(ChangeType.SIGNIFICANT_IMPROVEMENT);
        best.setPercentageChange(-30.0);
        BenchmarkComparison minor = createComparison(ChangeType.MINOR_IMPROVEMENT);
        minor.setPercentageChange(-7.0);

        ComparisonSummary summary = calculator.summarize(List.of(best, minor));

        assertThat(summary.bestImprovementPercent()).isEqualTo(-30.0);
    }

    @Test
    void summarize_countsNoBaseline() {
        List<BenchmarkComparison> comparisons = List.of(
                createComparison(ChangeType.NO_BASELINE),
                createComparison(ChangeType.NO_BASELINE),
                createComparison(ChangeType.NO_CHANGE)
        );

        ComparisonSummary summary = calculator.summarize(comparisons);

        assertThat(summary.noBaseline()).isEqualTo(2);
        assertThat(summary.total()).isEqualTo(3);
    }

    @Test
    void statisticalSignificance_detectsWhenIntervalsDoNotOverlap() {
        BenchmarkResult current = createResultWithConfidenceInterval("Method#test()", 150.0, new double[]{140.0, 160.0});
        BenchmarkResult baseline = createResultWithConfidenceInterval("Method#test()", 100.0, new double[]{90.0, 110.0});

        BenchmarkComparison result = calculator.compare(current, baseline);

        assertThat(result.isStatisticallySignificant()).isTrue();
    }

    @Test
    void statisticalSignificance_detectsWhenIntervalsOverlap() {
        BenchmarkResult current = createResultWithConfidenceInterval("Method#test()", 105.0, new double[]{95.0, 115.0});
        BenchmarkResult baseline = createResultWithConfidenceInterval("Method#test()", 100.0, new double[]{90.0, 110.0});

        BenchmarkComparison result = calculator.compare(current, baseline);

        assertThat(result.isStatisticallySignificant()).isFalse();
    }

    private BenchmarkResult createResult(String signatureId, double averageTime) {
        MethodSignature signature = new MethodSignature();
        String[] parts = signatureId.split("#");
        signature.setClassName(parts[0]);
        signature.setMethodName(parts[1].replace("()", ""));

        BenchmarkResult.ExecutionStats stats = new BenchmarkResult.ExecutionStats();
        stats.setAverageTime(averageTime);
        stats.setStandardDeviation(averageTime * 0.1);

        BenchmarkResult result = new BenchmarkResult();
        result.setMethodSignature(signature);
        result.setExecutionStats(stats);
        result.setSuccessful(true);

        return result;
    }

    private BenchmarkResult createResultWithConfidenceInterval(String signatureId, double averageTime, double[] interval) {
        BenchmarkResult result = createResult(signatureId, averageTime);
        result.getExecutionStats().setConfidenceInterval(interval);
        return result;
    }

    private BenchmarkComparison createComparison(ChangeType changeType) {
        return BenchmarkComparison.builder()
                .changeType(changeType)
                .percentageChange(0.0)
                .build();
    }
}