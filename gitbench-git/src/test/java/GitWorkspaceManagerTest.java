import git.GitService;
import git.GitWorkspaceManager;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class GitWorkspaceManagerTest {

    @TempDir
    Path tempDir;

    @TempDir
    Path workspaceDir;

    GitService gitService;
    GitWorkspaceManager workspaceManager;
    Git git;

    @BeforeEach
    void setup() throws Exception {
        git = Git.init().setDirectory(tempDir.toFile()).call();

        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "initial content");
        git.add().addFilepattern(".").call();
        git.commit().setMessage("Initial commit").call();

        gitService = new GitService(tempDir);
        workspaceManager = new GitWorkspaceManager(gitService, workspaceDir, false);
    }

    @AfterEach
    void teardown() {
        if (workspaceManager != null) {
            workspaceManager.close();
        }
        if (gitService != null) {
            gitService.close();
        }
        if (git != null) {
            git.close();
        }
    }

    @Test
    void constructor_createsWorkspaceDirectory() {
        assertThat(Files.exists(workspaceDir)).isTrue();
    }

    @Test
    void getWorkspaceForHead_returnsOriginalPath() {
        Path workspace = workspaceManager.getWorkspaceForHead();

        assertThat(workspace).isEqualTo(tempDir);
    }

    @Test
    void getWorkspaceForCommit_copiesFiles() {
        String commitHash = gitService.getCurrentCommit().getHash();

        Path workspace = workspaceManager.getWorkspaceForCommit(commitHash);

        assertThat(Files.exists(workspace.resolve("test.txt"))).isTrue();
    }

    @Test
    void hasWorkspace_returnsFalse_initially() {
        String commitHash = gitService.getCurrentCommit().getHash();

        assertThat(workspaceManager.hasWorkspace(commitHash)).isFalse();
    }

    @Test
    void hasWorkspace_returnsTrue_afterCreation() {
        String commitHash = gitService.getCurrentCommit().getHash();
        workspaceManager.getWorkspaceForCommit(commitHash);

        assertThat(workspaceManager.hasWorkspace(commitHash)).isTrue();
    }

    @Test
    void cleanupWorkspace_removesWorkspace() {
        String commitHash = gitService.getCurrentCommit().getHash();
        Path workspace = workspaceManager.getWorkspaceForCommit(commitHash);

        workspaceManager.cleanupWorkspace(commitHash);

        assertThat(workspaceManager.hasWorkspace(commitHash)).isFalse();
        assertThat(Files.exists(workspace)).isFalse();
    }

    @Test
    void getWorkspaceForCommit_checkoutsCorrectCommit() throws Exception {
        String firstHash = gitService.getCurrentCommit().getHash();

        Path file = tempDir.resolve("second.txt");
        Files.writeString(file, "second");
        git.add().addFilepattern(".").call();
        git.commit().setMessage("Second commit").call();

        Path workspace = workspaceManager.getWorkspaceForCommit(firstHash);

        assertThat(Files.exists(workspace.resolve("test.txt"))).isTrue();
        assertThat(Files.exists(workspace.resolve("second.txt"))).isFalse();
    }

    @Test
    void getWorkspaceForCommit_skipsTargetDirectory() throws Exception {
        Path targetDir = tempDir.resolve("target");
        Files.createDirectories(targetDir);
        Files.writeString(targetDir.resolve("build.txt"), "build output");

        String commitHash = gitService.getCurrentCommit().getHash();
        Path workspace = workspaceManager.getWorkspaceForCommit(commitHash);

        assertThat(Files.exists(workspace.resolve("target"))).isFalse();
    }
}
