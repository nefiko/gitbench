package runner;

import model.BenchmarkResult;
import model.CommitInfo;
import model.MethodSignature;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JmhRunner {

    private Path classDir;
    private String classpath;

    public JmhRunner() {
    }

    public JmhRunner(Path classDir, String classpath) {
        this.classDir = classDir;
        this.classpath = classpath;
    }

    public List<BenchmarkResult> run(List<String> benchmarkClasses, CommitInfo commit) {
        List<BenchmarkResult> results = new ArrayList<>();

        for (String benchmarkClass : benchmarkClasses) {
            BenchmarkResult result = runOne(benchmarkClass, commit);
            results.add(result);
        }

        return results;
    }

    private BenchmarkResult runOne(String benchmarkClass, CommitInfo commit) {
        try {
            String separator = File.pathSeparator;
            String fullClasspath = classDir.toString() + separator + classpath;

            System.out.println("  Running: " + benchmarkClass);

            ProcessBuilder pb = new ProcessBuilder(
                    "java",
                    "-cp", fullClasspath,
                    "org.openjdk.jmh.Main",
                    "-f", "1",
                    "-wi", "1",
                    "-i", "2",
                    "-tu", "ms",
                    "-r", "1s",
                    "-w", "1s",
                    benchmarkClass
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    if (line.contains("Iteration") || line.contains("Warmup")) {
                        System.out.print(".");
                    }
                }
            }
            System.out.println();

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                return createFailedResult(benchmarkClass, commit, "JMH exited with code " + exitCode + ": " + output);
            }

            return parseJmhOutput(output.toString(), benchmarkClass, commit);

        } catch (Exception e) {
            return createFailedResult(benchmarkClass, commit, e.getMessage());
        }
    }

    private BenchmarkResult parseJmhOutput(String output, String benchmarkClass, CommitInfo commit) {
        Pattern resultPattern = Pattern.compile(
                "([\\d.]+)\\s*±\\s*([\\d.]+)\\s+ms/op"
        );

        Matcher matcher = resultPattern.matcher(output);

        if (matcher.find()) {
            double mean = Double.parseDouble(matcher.group(1));
            double error = Double.parseDouble(matcher.group(2));
            return createSuccessResult(benchmarkClass, commit, mean, error);
        }

        Pattern simplePattern = Pattern.compile(
                "\\s([\\d.]+)\\s+ms/op"
        );
        Matcher simpleMatcher = simplePattern.matcher(output);

        if (simpleMatcher.find()) {
            double mean = Double.parseDouble(simpleMatcher.group(1));
            return createSuccessResult(benchmarkClass, commit, mean, 0);
        }

        Pattern sciPattern = Pattern.compile(
                "≈\\s*10([⁻⁰¹²³⁴⁵⁶⁷⁸⁹]+)\\s+ms/op"
        );
        Matcher sciMatcher = sciPattern.matcher(output);

        if (sciMatcher.find()) {
            String superscript = sciMatcher.group(1);
            int exponent = parseSuperscript(superscript);
            double mean = Math.pow(10, exponent);
            return createSuccessResult(benchmarkClass, commit, mean, 0);
        }

        Pattern sciPattern2 = Pattern.compile(
                "([\\d.]+)[Ee]([+-]?\\d+)\\s+ms/op"
        );
        Matcher sciMatcher2 = sciPattern2.matcher(output);

        if (sciMatcher2.find()) {
            double base = Double.parseDouble(sciMatcher2.group(1));
            int exp = Integer.parseInt(sciMatcher2.group(2));
            double mean = base * Math.pow(10, exp);
            return createSuccessResult(benchmarkClass, commit, mean, 0);
        }

        if (output.contains("ERROR") || output.contains("Exception")) {
            String errorMsg = extractError(output);
            return createFailedResult(benchmarkClass, commit, errorMsg);
        }

        return createFailedResult(benchmarkClass, commit, "Could not find benchmark result in output");
    }

    private BenchmarkResult createSuccessResult(String benchmarkClass, CommitInfo commit, double mean, double error) {
        MethodSignature signature = parseMethodSignature(benchmarkClass);

        BenchmarkResult.ExecutionStats stats = BenchmarkResult.ExecutionStats.builder()
                .averageTime(mean)
                .standardDeviation(error)
                .build();

        return BenchmarkResult.builder()
                .id(UUID.randomUUID().toString())
                .methodSignature(signature)
                .commitInfo(commit)
                .executionStats(stats)
                .runTimestamp(Instant.now())
                .timeUnit(TimeUnit.MILLISECONDS)
                .mode(BenchmarkResult.BenchmarkMode.AVERAGE_TIME)
                .successful(true)
                .build();
    }

    private String extractError(String output) {
        int errorIdx = output.indexOf("ERROR");
        if (errorIdx == -1) {
            errorIdx = output.indexOf("Exception");
        }
        if (errorIdx != -1) {
            int endIdx = Math.min(errorIdx + 200, output.length());
            return output.substring(errorIdx, endIdx);
        }
        return "Unknown error";
    }

    /* JMH uses superscript which needs to be translated into integer */
    private int parseSuperscript(String superscript) {
        StringBuilder num = new StringBuilder();
        boolean negative = false;

        for (char c : superscript.toCharArray()) {
            switch (c) {
                case '⁻' -> negative = true;
                case '⁰' -> num.append('0');
                case '¹' -> num.append('1');
                case '²' -> num.append('2');
                case '³' -> num.append('3');
                case '⁴' -> num.append('4');
                case '⁵' -> num.append('5');
                case '⁶' -> num.append('6');
                case '⁷' -> num.append('7');
                case '⁸' -> num.append('8');
                case '⁹' -> num.append('9');
            }
        }

        int value = !num.isEmpty() ? Integer.parseInt(num.toString()) : 0;
        return negative ? -value : value;
    }

    MethodSignature parseMethodSignature(String benchmarkClass) {
        String simpleName = benchmarkClass.substring(benchmarkClass.lastIndexOf('.') + 1);
        String withoutSuffix = simpleName.replace("_Benchmark", "");
        int lastUnderscore = withoutSuffix.lastIndexOf('_');

        String className = withoutSuffix.substring(0, lastUnderscore);
        String methodName = withoutSuffix.substring(lastUnderscore + 1);

        MethodSignature signature = new MethodSignature();
        signature.setClassName(className);
        signature.setMethodName(methodName);
        return signature;
    }

    private BenchmarkResult createFailedResult(String benchmarkClass, CommitInfo commit, String error) {
        MethodSignature signature = parseMethodSignature(benchmarkClass);

        return BenchmarkResult.builder()
                .id(UUID.randomUUID().toString())
                .methodSignature(signature)
                .commitInfo(commit)
                .runTimestamp(Instant.now())
                .successful(false)
                .errorMessage(error)
                .build();
    }
}