import config.ScannerConfiguration;
import enums.HotspotReason;
import model.HotspotCandidate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import scanner.impl.StaticCodeScanner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StaticCodeScannerTest {

    private StaticCodeScanner scanner;
    private ScannerConfiguration config;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() {
        scanner = new StaticCodeScanner();
        config = new ScannerConfiguration();
    }

    @Test
    void findsServiceAnnotatedClass() throws Exception {
        String code = """
                package com.example;
                
                import org.springframework.stereotype.Service;
                
                @Service
                public class UserService {
                    public void saveUser() {
                    }
                }
                """;

        createJavaFile("com/example/UserService.java", code);

        List<HotspotCandidate> results = scanner.scan(tempDir, config);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getReasons()).contains(HotspotReason.SERVICE_ANNOTATION);
    }

    @Test
    void findsControllerEndpoints() throws Exception {
        String code = """
                package com.example;
                
                import org.springframework.web.bind.annotation.RestController;
                import org.springframework.web.bind.annotation.GetMapping;
                
                @RestController
                public class UserController {
                    @GetMapping("/users")
                    public void getUsers() {
                    }
                }
                """;

        createJavaFile("com/example/UserController.java", code);

        List<HotspotCandidate> results = scanner.scan(tempDir, config);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getReasons()).contains(HotspotReason.CONTROLLER_ENDPOINT);
    }

    @Test
    void findsLoops() throws Exception {
        String code = """
                package com.example;
                
                import org.springframework.stereotype.Service;
                
                @Service
                public class Calculator {
                    public int sum(int[] numbers) {
                        int total = 0;
                        for (int n : numbers) {
                            total += n;
                        }
                        return total;
                    }
                }
                """;

        createJavaFile("com/example/Calculator.java", code);

        List<HotspotCandidate> results = scanner.scan(tempDir, config);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getReasons()).contains(HotspotReason.CONTAINS_LOOP);
        assertThat(results.get(0).getComplexityMetrics().getNumberOfLoops()).isEqualTo(1);
    }

    @Test
    void calculatesComplexity() throws Exception {
        String code = """
                package com.example;
                
                import org.springframework.stereotype.Service;
                
                @Service
                public class ComplexService {
                    public String process(int x) {
                        if (x > 10) {
                            if (x > 20) {
                                return "big";
                            }
                            return "medium";
                        } else if (x > 5) {
                            return "small";
                        } else {
                            return "tiny";
                        }
                    }
                }
                """;

        createJavaFile("com/example/ComplexService.java", code);
        config.setMinComplexity(3);

        List<HotspotCandidate> results = scanner.scan(tempDir, config);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getReasons()).contains(HotspotReason.HIGH_CYCLOMATIC_COMPLEXITY);
        assertThat(results.get(0).getComplexityMetrics().getCyclomaticComplexity()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void ignoresPrivateMethods() throws Exception {
        String code = """
                package com.example;
                
                import org.springframework.stereotype.Service;
                
                @Service
                public class MyService {
                    public void publicMethod() {
                    }
                    
                    private void privateMethod() {
                    }
                }
                """;

        createJavaFile("com/example/MyService.java", code);

        List<HotspotCandidate> results = scanner.scan(tempDir, config);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMethodSignature().getMethodName()).isEqualTo("publicMethod");
    }

    @Test
    void ignoresInterfaces() throws Exception {
        String code = """
                package com.example;
                
                public interface UserRepository {
                    void save();
                }
                """;

        createJavaFile("com/example/UserRepository.java", code);

        List<HotspotCandidate> results = scanner.scan(tempDir, config);

        assertThat(results).isEmpty();
    }

    @Test
    void respectsBlacklist() throws Exception {
        String code = """
                package com.example;
                
                import org.springframework.stereotype.Service;
                
                @Service
                public class MyService {
                    public void doWork() {
                    }
                }
                """;

        createJavaFile("com/example/MyService.java", code);
        config.getBlacklist().add("com.example.MyService#doWork()");

        List<HotspotCandidate> results = scanner.scan(tempDir, config);

        assertThat(results).isEmpty();
    }

    @Test
    void respectsWhitelist() throws Exception {
        String code = """
                package com.example;
                
                public class PlainClass {
                    public void plainMethod() {
                    }
                }
                """;

        createJavaFile("com/example/PlainClass.java", code);
        config.getWhitelist().add("com.example.PlainClass#plainMethod()");

        List<HotspotCandidate> results = scanner.scan(tempDir, config);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).isManuallyAdded()).isTrue();
    }

    @Test
    void excludesPackages() throws Exception {
        String code = """
                package com.example.internal;
                
                import org.springframework.stereotype.Service;
                
                @Service
                public class InternalService {
                    public void doWork() {
                    }
                }
                """;

        createJavaFile("com/example/internal/InternalService.java", code);
        config.getExcludePackages().add("com.example.internal");

        List<HotspotCandidate> results = scanner.scan(tempDir, config);

        assertThat(results).isEmpty();
    }

    @Test
    void limitsMaxMethods() throws Exception {
        String code = """
                package com.example;
                
                import org.springframework.stereotype.Service;
                
                @Service
                public class BigService {
                    public void method1() {}
                    public void method2() {}
                    public void method3() {}
                    public void method4() {}
                    public void method5() {}
                }
                """;

        createJavaFile("com/example/BigService.java", code);
        config.setMaxMethods(3);

        List<HotspotCandidate> results = scanner.scan(tempDir, config);

        assertThat(results).hasSize(3);
    }

    @Test
    void returnsEmptyForEmptyProject() throws Exception {
        List<HotspotCandidate> results = scanner.scan(tempDir, config);

        assertThat(results).isEmpty();
    }

    @Test
    void findsTransactionalMethods() throws Exception {
        String code = """
                package com.example;
                
                import org.springframework.stereotype.Service;
                import org.springframework.transaction.annotation.Transactional;
                
                @Service
                public class OrderService {
                    @Transactional
                    public void placeOrder() {
                    }
                }
                """;

        createJavaFile("com/example/OrderService.java", code);

        List<HotspotCandidate> results = scanner.scan(tempDir, config);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getReasons()).contains(HotspotReason.TRANSACTIONAL_METHOD);
    }

    @Test
    void measuresNestingDepth() throws Exception {
        String code = """
                package com.example;
                
                import org.springframework.stereotype.Service;
                
                @Service
                public class DeepService {
                    public void deep(int x) {
                        if (x > 0) {
                            if (x > 1) {
                                if (x > 2) {
                                    System.out.println("deep");
                                }
                            }
                        }
                    }
                }
                """;

        createJavaFile("com/example/DeepService.java", code);
        config.setMinComplexity(1);

        List<HotspotCandidate> results = scanner.scan(tempDir, config);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getComplexityMetrics().getNestingDepth()).isEqualTo(3);
    }

    private void createJavaFile(String relativePath, String content) throws Exception {
        Path srcDir = tempDir.resolve("src/main/java");
        Path file = srcDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}