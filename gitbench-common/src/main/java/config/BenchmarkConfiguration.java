package config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BenchmarkConfiguration {

    private List<BenchmarkMethodConfiguration> benchmarks = new ArrayList<>();

    @Data
    public static class BenchmarkMethodConfiguration {
        private String className;
        private String methodName;
        private String setup;
        private List<String> params = new ArrayList<>();
        private int warmupIterations = 3;
        private int measurementIterations = 5;
    }
}

