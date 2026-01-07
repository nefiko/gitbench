package compiler;

import generator.BenchmarkGenerator.GeneratedBenchmark;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

        List<String> sourceFiles = new ArrayList<>();
        for (GeneratedBenchmark benchmark : benchmarks) {
            String fileName = benchmark.className().replace(".", "/") + ".java";
            Path sourceFile = sourceDir.resolve(fileName);
            Files.createDirectories(sourceFile.getParent());
            Files.writeString(sourceFile, benchmark.sourceCode());
            sourceFiles.add(sourceFile.toString());
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new RuntimeException("No Java compiler available. Run with JDK, not JRE.");
        }

        List<String> args = new ArrayList<>();
        args.add("-cp");
        args.add(classpath);
        args.add("-d");
        args.add(classDir.toString());
        args.add("-s");
        args.add(sourceDir.toString());
        args.add("-proc:full");
        args.addAll(sourceFiles);

        int result = compiler.run(null, null, null, args.toArray(new String[0]));

        if (result != 0) {
            throw new RuntimeException("Compilation failed for benchmarks");
        }
    }

    public Path getClassDir() {
        return outputDir.resolve("classes");
    }

    public Path getSourceDir() {
        return outputDir.resolve("src");
    }
}