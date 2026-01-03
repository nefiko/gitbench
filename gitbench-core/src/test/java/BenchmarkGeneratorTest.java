import config.BenchmarkConfiguration;
import generator.BenchmarkGenerator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BenchmarkGeneratorTest {

    @Test
    void generate_returnsEmptyList_whenNoMethodsConfigured() {
        BenchmarkGenerator generator = new BenchmarkGenerator();
        BenchmarkConfiguration config = new BenchmarkConfiguration();

        List<BenchmarkGenerator.GeneratedBenchmark> result = generator.generate(config);

        assertThat(result).isEmpty();
    }

    @Test
    void generate_returnsOneBenchmark_whenOneMethodConfigured() {
        BenchmarkGenerator generator = new BenchmarkGenerator();
        BenchmarkConfiguration config = createConfigWithOneMethod();

        List<BenchmarkGenerator.GeneratedBenchmark> result = generator.generate(config);

        assertThat(result).hasSize(1);
    }

    @Test
    void generate_producesCorrectClassName() {
        BenchmarkGenerator generator = new BenchmarkGenerator();
        BenchmarkConfiguration config = createConfigWithOneMethod();

        List<BenchmarkGenerator.GeneratedBenchmark> result = generator.generate(config);

        assertThat(result.get(0).className()).isEqualTo("generated.Calculator_add_Benchmark");
    }

    @Test
    void generate_producesCodeWithJmhAnnotations() {
        BenchmarkGenerator generator = new BenchmarkGenerator();
        BenchmarkConfiguration config = createConfigWithOneMethod();

        List<BenchmarkGenerator.GeneratedBenchmark> result = generator.generate(config);
        String code = result.get(0).sourceCode();

        assertThat(code).contains("@Benchmark");
        assertThat(code).contains("@State(Scope.Benchmark)");
        assertThat(code).contains("@Setup(Level.Trial)");
    }

    @Test
    void generate_includesMethodCall() {
        BenchmarkGenerator generator = new BenchmarkGenerator();
        BenchmarkConfiguration config = createConfigWithOneMethod();

        List<BenchmarkGenerator.GeneratedBenchmark> result = generator.generate(config);
        String code = result.get(0).sourceCode();

        assertThat(code).contains("instance.add(1, 2)");
    }

    private BenchmarkConfiguration createConfigWithOneMethod() {
        BenchmarkConfiguration config = new BenchmarkConfiguration();

        BenchmarkConfiguration.BenchmarkMethodConfiguration method = new BenchmarkConfiguration.BenchmarkMethodConfiguration();
        method.setClassName("com.example.Calculator");
        method.setMethodName("add");
        method.setParams(List.of("1", "2"));

        config.getBenchmarks().add(method);
        return config;
    }
}