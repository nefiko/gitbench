package config;

import lombok.Data;

import java.util.concurrent.TimeUnit;

@Data
public class BenchmarkConfiguration {
    private int warmupIterations = 3;
    private int measurementIterations = 5;
    private int forks = 1;
    private TimeUnit timeUnit = TimeUnit.MILLISECONDS;
    private double regressionThreshold = 10.0;
}

