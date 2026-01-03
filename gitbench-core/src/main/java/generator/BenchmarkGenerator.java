package generator;

import config.BenchmarkConfiguration;
import lombok.Data;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class BenchmarkGenerator {

    private final String template;

    public BenchmarkGenerator() {
        this.template = loadTemplate();
    }

    public List<GeneratedBenchmark> generate(BenchmarkConfiguration config) {
        List<GeneratedBenchmark> benchmarks = new ArrayList<>();

        for (BenchmarkConfiguration.BenchmarkMethodConfiguration method : config.getBenchmarks()) {
            String code = generateBenchmarkClass(method);
            String className = getBenchmarkClassName(method);
            benchmarks.add(new GeneratedBenchmark(className, code));
        }

        return benchmarks;
    }

    private String generateBenchmarkClass(BenchmarkConfiguration.BenchmarkMethodConfiguration method) {
        String simpleClassName = getSimpleClassName(method.getClassName());
        String benchmarkClassName = simpleClassName + "_" + method.getMethodName() + "_Benchmark";
        String params = String.join(", ", method.getParams());
        String setup = getSetupCode(method);

        return template
                .replace("${imports}", "")
                .replace("${warmupIterations}", String.valueOf(method.getWarmupIterations()))
                .replace("${measurementIterations}", String.valueOf(method.getMeasurementIterations()))
                .replace("${benchmarkClassName}", benchmarkClassName)
                .replace("${className}", method.getClassName())
                .replace("${setup}", setup)
                .replace("${methodName}", method.getMethodName())
                .replace("${params}", params);
    }

    private String getSetupCode(BenchmarkConfiguration.BenchmarkMethodConfiguration method) {
        if (method.getSetup() != null && !method.getSetup().startsWith("//")) {
            return method.getSetup();
        }
        return "new " + method.getClassName() + "()";
    }

    private String getBenchmarkClassName(BenchmarkConfiguration.BenchmarkMethodConfiguration method) {
        String simpleClassName = getSimpleClassName(method.getClassName());
        return "generated." + simpleClassName + "_" + method.getMethodName() + "_Benchmark";
    }

    private String getSimpleClassName(String fullClassName) {
        int lastDot = fullClassName.lastIndexOf('.');
        if (lastDot >= 0) {
            return fullClassName.substring(lastDot + 1);
        }
        return fullClassName;
    }

    private String loadTemplate() {
        try (InputStream is = getClass().getResourceAsStream("/templates/benchmark.template")) {
            if (is == null) {
                throw new RuntimeException("Template file not found");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load template", e);
        }
    }

    public record GeneratedBenchmark(String className, String sourceCode) {
    }
}