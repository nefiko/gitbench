import exception.GitOperationException;
import lombok.Data;
import model.CommitInfo;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Data
public class GitWorkspaceManager implements Closeable {

    private final GitService gitService;
    private final Path baseWorkspacePath;
    private final Map<String, Path> workspaces = new HashMap<>();
    private final boolean cleanupOnClose;
    private final String originalBranch;

    public GitWorkspaceManager(GitService gitService, Path workspacePath, boolean cleanupOnClose) {
        this.gitService = gitService;
        this.baseWorkspacePath = workspacePath;
        this.cleanupOnClose = cleanupOnClose;

        try {
            Files.createDirectories(baseWorkspacePath);
            this.originalBranch = gitService.getCurrentBranch();
        } catch (IOException e) {
            throw new GitOperationException("Failed to create workspace directory", e);
        }
    }

    public boolean hasWorkspace(String commitHash) {
        return workspaces.containsKey(commitHash);
    }

    public Path getWorkspaceForCommit(String commitRef) {
        CommitInfo commitInfo = gitService.getCommitInfo(commitRef);
        String hash = commitInfo.getHash();

        if (workspaces.containsKey(hash)) {
            return workspaces.get(hash);
        }

        Path workspacePath = baseWorkspacePath.resolve("commit-" + commitInfo.getShortHash());

        try {
            if (Files.exists(workspacePath)) {
                deleteDirectoryRecursively(workspacePath);
            }
            Files.createDirectories(workspacePath);

            copyRepository(gitService.getRepositoryPath(), workspacePath);

            try (GitService workspaceGit = new GitService(workspacePath)) {
                workspaceGit.checkout(hash);
            }

            workspaces.put(hash, workspacePath);
            return workspacePath;

        } catch (IOException e) {
            throw new GitOperationException("Failed to create workspace for commit: " + commitRef, e);
        }
    }

    public Path getWorkspaceForHead() {
        return gitService.getRepositoryPath();
    }

    public void cleanupWorkspace(String commitHash) {
        Path workspace = workspaces.remove(commitHash);
        if (workspace != null) {
            try {
                deleteDirectoryRecursively(workspace);
            } catch (IOException e) {
            }
        }
    }

    public void cleanupAllWorkspaces() {
        for (Path workspace : workspaces.values()) {
            try {
                deleteDirectoryRecursively(workspace);
            } catch (IOException e) {
            }
        }
        workspaces.clear();
    }

    public void restoreOriginalState() {
        if (originalBranch != null) {
            try {
                gitService.checkoutBranch(originalBranch);
            } catch (Exception e) {
            }
        }
    }

    private void copyRepository(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                String dirName = dir.getFileName().toString();

                if (dirName.equals("target") || dirName.equals("build") ||
                        dirName.equals("node_modules") || dirName.equals(".gitbench") ||
                        dirName.equals("out") || dirName.equals(".gradle")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path targetFile = target.resolve(source.relativize(file));
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void deleteDirectoryRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                        }
                    });
        }
    }

    @Override
    public void close() {
        if (originalBranch != null) {
            try {
                gitService.checkoutBranch(originalBranch);
            } catch (Exception _) {
            }
        }

        if (cleanupOnClose) {
            for (Path workspace : workspaces.values()) {
                try {
                    deleteDirectoryRecursively(workspace);
                } catch (IOException _) {
                }
            }
            workspaces.clear();

            try {
                deleteDirectoryRecursively(baseWorkspacePath);
            } catch (IOException _) {
            }
        }
    }
}
