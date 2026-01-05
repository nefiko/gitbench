
import model.BenchmarkResult;
import model.CommitInfo;
import model.MethodSignature;
import org.junit.jupiter.api.Test;
import runner.JmhRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JmhRunnerTest {

    @Test
    void run_returnsEmptyList_whenNoBenchmarks() {
        JmhRunner runner = new JmhRunner();
        CommitInfo commit = createCommit();

        List<BenchmarkResult> results = runner.run(List.of(), commit);

        assertThat(results).isEmpty();
    }

    @Test
    void run_returnsFailedResult_whenBenchmarkClassNotFound() {
        JmhRunner runner = new JmhRunner();
        CommitInfo commit = createCommit();

        List<BenchmarkResult> results = runner.run(List.of("generated.NonExistent_method_Benchmark"), commit);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).isSuccessful()).isFalse();
    }

    @Test
    void parseMethodSignature_extractsClassName() {
        JmhRunner runner = new JmhRunner();

        MethodSignature signature = runner.parseMethodSignature("generated.Calculator_add_Benchmark");

        assertThat(signature.getClassName()).isEqualTo("Calculator");
    }

    @Test
    void parseMethodSignature_extractsMethodName() {
        JmhRunner runner = new JmhRunner();

        MethodSignature signature = runner.parseMethodSignature("generated.Calculator_add_Benchmark");

        assertThat(signature.getMethodName()).isEqualTo("add");
    }

    @Test
    void parseMethodSignature_handlesUnderscoreInClassName() {
        JmhRunner runner = new JmhRunner();

        MethodSignature signature = runner.parseMethodSignature("generated.User_Service_process_Benchmark");

        assertThat(signature.getClassName()).isEqualTo("User_Service");
        assertThat(signature.getMethodName()).isEqualTo("process");
    }

    @Test
    void run_includesCommitInfo_inFailedResult() {
        JmhRunner runner = new JmhRunner();
        CommitInfo commit = createCommit();

        List<BenchmarkResult> results = runner.run(List.of("generated.Test_method_Benchmark"), commit);

        assertThat(results.get(0).getCommitInfo()).isEqualTo(commit);
    }

    @Test
    void run_setsTimestamp_inFailedResult() {
        JmhRunner runner = new JmhRunner();
        CommitInfo commit = createCommit();

        List<BenchmarkResult> results = runner.run(List.of("generated.Test_method_Benchmark"), commit);

        assertThat(results.get(0).getRunTimestamp()).isNotNull();
    }

    @Test
    void run_generatesUniqueId_forEachResult() {
        JmhRunner runner = new JmhRunner();
        CommitInfo commit = createCommit();

        List<BenchmarkResult> results = runner.run(
                List.of("generated.First_method_Benchmark", "generated.Second_method_Benchmark"),
                commit
        );

        assertThat(results.get(0).getId()).isNotEqualTo(results.get(1).getId());
    }

    private CommitInfo createCommit() {
        CommitInfo commit = new CommitInfo();
        commit.setHash("abc123def456");
        commit.setMessage("Test commit");
        commit.setAuthorName("Test Author");
        return commit;
    }
}