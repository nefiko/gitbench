import model.CommitInfo;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GitServiceTest {

    @TempDir
    Path tempDir;

    GitService gitService;
    Git git;

    @BeforeEach
    void setup() throws Exception {
        git = Git.init().setDirectory(tempDir.toFile()).call();

        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "initial content");
        git.add().addFilepattern(".").call();
        git.commit().setMessage("Initial commit").call();

        gitService = new GitService(tempDir);
    }

    @AfterEach
    void teardown() {
        if (gitService != null) {
            gitService.close();
        }
        if (git != null) {
            git.close();
        }
    }

    @Test
    void getCurrentCommit_returnsHeadCommit() {
        CommitInfo commit = gitService.getCurrentCommit();

        assertThat(commit).isNotNull();
        assertThat(commit.getHash()).isNotEmpty();
        assertThat(commit.getMessage()).contains("Initial commit");
    }

    @Test
    void getCommitInfo_resolvesHead() {
        CommitInfo commit = gitService.getCommitInfo("HEAD");

        assertThat(commit).isNotNull();
        assertThat(commit.getShortHash()).hasSize(7);
    }

    @Test
    void getCommitInfo_resolvesHash() {
        CommitInfo head = gitService.getCurrentCommit();
        CommitInfo byHash = gitService.getCommitInfo(head.getHash());

        assertThat(byHash.getHash()).isEqualTo(head.getHash());
    }

    @Test
    void getParentCommit_returnsEmpty_forFirstCommit() {
        Optional<CommitInfo> parent = gitService.getParentCommit("HEAD");

        assertThat(parent).isEmpty();
    }

    @Test
    void getParentCommit_returnsParent() throws Exception {
        Path file = tempDir.resolve("second.txt");
        Files.writeString(file, "second content");
        git.add().addFilepattern(".").call();
        git.commit().setMessage("Second commit").call();

        Optional<CommitInfo> parent = gitService.getParentCommit("HEAD");

        assertThat(parent).isPresent();
        assertThat(parent.get().getMessage()).contains("Initial commit");
    }
    
    @Test
    void getChangedJavaFiles_returnsOnlyJavaFiles() throws Exception {
        String firstCommit = gitService.getCurrentCommit().getHash();

        Path javaFile = tempDir.resolve("Test.java");
        Files.writeString(javaFile, "public class Test {}");
        Path txtFile = tempDir.resolve("readme.txt");
        Files.writeString(txtFile, "readme");
        git.add().addFilepattern(".").call();
        git.commit().setMessage("Add files").call();

        String secondCommit = gitService.getCurrentCommit().getHash();

        List<String> javaFiles = gitService.getChangedJavaFiles(firstCommit, secondCommit);

        assertThat(javaFiles).containsExactly("Test.java");
    }
}
