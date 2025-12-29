package entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "commits", indexes = {
        @Index(name = "idx_commit_hash", columnList = "hash"),
        @Index(name = "idx_commit_project", columnList = "project_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Column(nullable = false, length = 40)
    private String hash;

    @Column(name = "short_hash", nullable = false, length = 7)
    private String shortHash;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "author_name")
    private String authorName;

    @Column(name = "author_email")
    private String authorEmail;

    @Column(name = "commit_timestamp")
    private Instant commitTimestamp;

    @Column(name = "parent_hash", length = 40)
    private String parentHash;

    @Column(name = "is_merge")
    @Builder.Default
    private boolean merge = false;

    @OneToMany(mappedBy = "commit", cascade = CascadeType.ALL)
    @Builder.Default
    private List<BenchmarkRunEntity> benchmarkRuns = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (hash != null && shortHash == null) {
            shortHash = hash.substring(0, 7);
        }
    }
}