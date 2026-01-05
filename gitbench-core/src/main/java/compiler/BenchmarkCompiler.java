package compiler;

import generator.BenchmarkGenerator.GeneratedBenchmark;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class BenchmarkCompiler {

    private final Path outputDir;

    public BenchmarkCompiler(Path outputDir) {
        this.outputDir = outputDir;
    }

    public void compile(List<GeneratedBenchmark> benchmarks, String classpath) throws IOException {
        Path sourceDir = outputDir.resolve("src");
        Path classDir = outputDir.resolve("classes");

        Files.createDirectories(sourceDir.resolve("generated"));
        Files.createDirectories(classDir);

        for (GeneratedBenchmark benchmark : benchmarks) {
            String fileName = benchmark.className().replace(".", "/") + ".java";
            Path sourceFile = sourceDir.resolve(fileName);
            Files.createDirectories(sourceFile.getParent());
            Files.writeString(sourceFile, benchmark.sourceCode());
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new RuntimeException("No Java compiler available");
        }

        for (GeneratedBenchmark benchmark : benchmarks) {
            String fileName = benchmark.className().replace(".", "/") + ".java";
            Path sourceFile = sourceDir.resolve(fileName);

            int result = compiler.run(
                    null, null, null,
                    "-cp", classpath,
                    "-d", classDir.toString(),
                    sourceFile.toString()
            );

            if (result != 0) {
                throw new RuntimeException("Compilation failed for: " + benchmark.className());
            }
        }
    }

    public Path getClassDir() {
        return outputDir.resolve("classes");
    }

    public Path getSourceDir() {
        return outputDir.resolve("src");
    }
}