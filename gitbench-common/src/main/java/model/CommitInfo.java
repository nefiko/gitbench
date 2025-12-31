package model;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class CommitInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String hash;

    private String shortHash;

    private String message;

    private String authorName;

    private String authorEmail;

    private Instant timestamp;

    private List<String> parentHashes;

    private List<String> branches;

    public boolean isMergeCommit() {
        return parentHashes != null && parentHashes.size() > 1;
    }

    public String getFirstParentHash() {
        if (parentHashes != null && !parentHashes.isEmpty()) {
            return parentHashes.getFirst();
        }
        return null;
    }
}