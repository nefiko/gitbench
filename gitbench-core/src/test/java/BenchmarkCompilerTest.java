import compiler.BenchmarkCompiler;
import generator.BenchmarkGenerator.GeneratedBenchmark;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BenchmarkCompilerTest {

    @TempDir
    Path tempDir;

    @Test
    void constructor_createsOutputDir() {
        BenchmarkCompiler compiler = new BenchmarkCompiler(tempDir);

        assertThat(compiler.getClassDir()).isEqualTo(tempDir.resolve("classes"));
        assertThat(compiler.getSourceDir()).isEqualTo(tempDir.resolve("src"));
    }

    @Test
    void compile_createsSourceFile() throws Exception {
        BenchmarkCompiler compiler = new BenchmarkCompiler(tempDir);
        GeneratedBenchmark benchmark = createSimpleBenchmark();

        compiler.compile(List.of(benchmark), System.getProperty("java.class.path"));

        Path sourceFile = tempDir.resolve("src/generated/Simple_test_Benchmark.java");
        assertThat(Files.exists(sourceFile)).isTrue();
    }

    @Test
    void compile_createsClassFile() throws Exception {
        BenchmarkCompiler compiler = new BenchmarkCompiler(tempDir);
        GeneratedBenchmark benchmark = createSimpleBenchmark();

        compiler.compile(List.of(benchmark), System.getProperty("java.class.path"));

        Path classFile = tempDir.resolve("classes/generated/Simple_test_Benchmark.class");
        assertThat(Files.exists(classFile)).isTrue();
    }

    @Test
    void compile_handlesMultipleBenchmarks() throws Exception {
        BenchmarkCompiler compiler = new BenchmarkCompiler(tempDir);
        List<GeneratedBenchmark> benchmarks = List.of(
                createBenchmark("generated.First_method_Benchmark", "First", "method"),
                createBenchmark("generated.Second_method_Benchmark", "Second", "method")
        );

        compiler.compile(benchmarks, System.getProperty("java.class.path"));

        assertThat(Files.exists(tempDir.resolve("classes/generated/First_method_Benchmark.class"))).isTrue();
        assertThat(Files.exists(tempDir.resolve("classes/generated/Second_method_Benchmark.class"))).isTrue();
    }

    @Test
    void compile_throwsException_whenCodeInvalid() {
        BenchmarkCompiler compiler = new BenchmarkCompiler(tempDir);
        GeneratedBenchmark invalidBenchmark = new GeneratedBenchmark(
                "generated.Invalid_Benchmark",
                "this is not valid java code"
        );

        assertThatThrownBy(() -> compiler.compile(List.of(invalidBenchmark), ""))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Compilation failed");
    }

    private GeneratedBenchmark createSimpleBenchmark() {
        return createBenchmark("generated.Simple_test_Benchmark", "Simple", "test");
    }

    private GeneratedBenchmark createBenchmark(String className, String simpleClassName, String methodName) {
        String code = """
                package generated;
                
                public class %s_%s_Benchmark {
                    public void benchmark() {
                    }
                }
                """.formatted(simpleClassName, methodName);

        return new GeneratedBenchmark(className, code);
    }
}