package git;

import exception.GitOperationException;
import lombok.Data;
import model.CommitInfo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Data
public class GitService implements Closeable {

    private final Repository repository;
    private final Git git;
    private final Path repositoryPath;

    public GitService(Path projectPath) {
        this.repositoryPath = projectPath;
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            this.repository = builder
                    .setGitDir(projectPath.resolve(".git").toFile())
                    .readEnvironment()
                    .findGitDir()
                    .build();
            this.git = new Git(repository);
        } catch (IOException e) {
            throw new GitOperationException("Failed to open Git repository at: " + projectPath, e);
        }
    }

    public CommitInfo getCommitInfo(String commitRef) {
        try {
            ObjectId objectId = repository.resolve(commitRef);
            if (objectId == null) {
                throw new GitOperationException("Cannot resolve commit reference: " + commitRef);
            }

            try (RevWalk revWalk = new RevWalk(repository)) {
                RevCommit commit = revWalk.parseCommit(objectId);
                return toCommitInfo(commit);
            }
        } catch (IOException e) {
            throw new GitOperationException("Failed to get commit info for: " + commitRef, e);
        }
    }

    public CommitInfo getCurrentCommit() {
        return getCommitInfo("HEAD");
    }

    public Optional<CommitInfo> getParentCommit(String commitRef) {
        try {
            ObjectId objectId = repository.resolve(commitRef);
            if (objectId == null) {
                throw new GitOperationException("Cannot resolve commit reference: " + commitRef);
            }

            try (RevWalk revWalk = new RevWalk(repository)) {
                RevCommit commit = revWalk.parseCommit(objectId);
                if (commit.getParentCount() > 0) {
                    RevCommit parent = revWalk.parseCommit(commit.getParent(0).getId());
                    return Optional.of(toCommitInfo(parent));
                }
                return Optional.empty();
            }
        } catch (IOException e) {
            throw new GitOperationException("Failed to get parent commit for: " + commitRef, e);
        }
    }

    public List<CommitInfo> getCommitsBetween(String fromRef, String toRef) {
        try {
            ObjectId fromId = repository.resolve(fromRef);
            ObjectId toId = repository.resolve(toRef);

            if (fromId == null || toId == null) {
                throw new GitOperationException("Cannot resolve commit references");
            }

            Iterable<RevCommit> commits = git.log()
                    .addRange(fromId, toId)
                    .call();

            return StreamSupport.stream(commits.spliterator(), false)
                    .map(this::toCommitInfo)
                    .collect(Collectors.toList());
        } catch (IOException | GitAPIException e) {
            throw new GitOperationException("Failed to get commits between: " + fromRef + " and " + toRef, e);
        }
    }

    public List<String> getChangedFiles(String fromRef, String toRef) {
        try {
            ObjectId fromId = repository.resolve(fromRef);
            ObjectId toId = repository.resolve(toRef);

            if (fromId == null || toId == null) {
                throw new GitOperationException("Cannot resolve commit references");
            }

            try (ObjectReader reader = repository.newObjectReader()) {
                CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
                try (RevWalk revWalk = new RevWalk(repository)) {
                    RevCommit oldCommit = revWalk.parseCommit(fromId);
                    oldTreeParser.reset(reader, oldCommit.getTree());
                }

                CanonicalTreeParser newTreeParser = new CanonicalTreeParser();
                try (RevWalk revWalk = new RevWalk(repository)) {
                    RevCommit newCommit = revWalk.parseCommit(toId);
                    newTreeParser.reset(reader, newCommit.getTree());
                }

                List<DiffEntry> diffs = git.diff()
                        .setOldTree(oldTreeParser)
                        .setNewTree(newTreeParser)
                        .call();

                return diffs.stream()
                        .map(DiffEntry::getNewPath)
                        .collect(Collectors.toList());
            }
        } catch (IOException | GitAPIException e) {
            throw new GitOperationException("Failed to get changed files", e);
        }
    }

    public List<String> getChangedJavaFiles(String fromRef, String toRef) {
        return getChangedFiles(fromRef, toRef).stream()
                .filter(file -> file.endsWith(".java"))
                .collect(Collectors.toList());
    }

    private CommitInfo toCommitInfo(RevCommit commit) {
        List<String> parentHashes = new ArrayList<>();
        for (RevCommit parent : commit.getParents()) {
            parentHashes.add(parent.getId().getName());
        }

        CommitInfo info = new CommitInfo();
        info.setHash(commit.getId().getName());
        info.setShortHash(commit.getId().abbreviate(7).name());
        info.setMessage(commit.getFullMessage());
        info.setAuthorName(commit.getAuthorIdent().getName());
        info.setAuthorEmail(commit.getAuthorIdent().getEmailAddress());
        info.setTimestamp(Instant.ofEpochSecond(commit.getCommitTime()));
        info.setParentHashes(parentHashes);
        return info;
    }

    public List<CommitInfo> getCommitHistory(String startRef, int maxCount) {
        try {
            ObjectId objectId = repository.resolve(startRef);
            if (objectId == null) {
                throw new GitOperationException("Cannot resolve commit reference: " + startRef);
            }

            Iterable<RevCommit> commits = git.log()
                    .add(objectId)
                    .setMaxCount(maxCount)
                    .call();

            return StreamSupport.stream(commits.spliterator(), false)
                    .map(this::toCommitInfo)
                    .collect(Collectors.toList());
        } catch (IOException | GitAPIException e) {
            throw new GitOperationException("Failed to get commit history from: " + startRef, e);
        }
    }

    public boolean hasUncommittedChanges() {
        try {
            return !git.status().call().isClean();
        } catch (GitAPIException e) {
            throw new GitOperationException("Failed to check repository status", e);
        }
    }

    public String getCurrentBranch() {
        try {
            return repository.getBranch();
        } catch (IOException e) {
            throw new GitOperationException("Failed to get current branch", e);
        }
    }

    public void checkout(String commitRef) {
        try {
            git.checkout()
                    .setName(commitRef)
                    .call();
        } catch (GitAPIException e) {
            throw new GitOperationException("Failed to checkout: " + commitRef, e);
        }
    }

    public void checkoutBranch(String branchName) {
        try {
            git.checkout()
                    .setName(branchName)
                    .call();
        } catch (GitAPIException e) {
            throw new GitOperationException("Failed to checkout branch: " + branchName, e);
        }
    }

    @Override
    public void close() {
        git.close();
        repository.close();
    }
}
