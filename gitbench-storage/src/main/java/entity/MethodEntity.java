package entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "methods", indexes = {
        @Index(name = "idx_method_signature", columnList = "signature_id"),
        @Index(name = "idx_method_project", columnList = "project_id"),
        @Index(name = "idx_method_class", columnList = "class_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MethodEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Column(name = "signature_id", nullable = false)
    private String signatureId;

    @Column(name = "class_name", nullable = false)
    private String className;

    @Column(name = "method_name", nullable = false)
    private String methodName;

    @Column(name = "parameter_types")
    private String parameterTypes;

    @Column(name = "return_type")
    private String returnType;

    @Column(name = "source_file")
    private String sourceFile;

    @Column(name = "whitelisted")
    @Builder.Default
    private boolean whitelisted = false;

    @Column(name = "blacklisted")
    @Builder.Default
    private boolean blacklisted = false;

    @Column(name = "priority_score")
    @Builder.Default
    private int priorityScore = 0;

    @Column(name = "hotspot_reasons")
    private String hotspotReasons;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @OneToMany(mappedBy = "method", cascade = CascadeType.ALL)
    @Builder.Default
    private List<BenchmarkResultEntity> results = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}