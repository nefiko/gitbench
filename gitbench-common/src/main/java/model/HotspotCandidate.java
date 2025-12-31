package model;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;


@Data
@Builder
public class HotspotCandidate implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private MethodSignature methodSignature;

    @Builder.Default
    private Set<HotspotReason> reasons = new HashSet<>();

    private int priorityScore;

    private boolean manuallyAdded;

    @Builder.Default
    private Set<String> annotations = new HashSet<>();

    private ComplexityMetrics complexityMetrics;

    public enum HotspotReason {
        SERVICE_ANNOTATION,          // @Service annotated class
        REPOSITORY_ANNOTATION,       // @Repository annotated class
        CONTROLLER_ENDPOINT,         // @GetMapping, @PostMapping, etc.
        CONTAINS_LOOP,               // Contains for/while loops
        CONTAINS_SQL_QUERY,          // Contains SQL operations
        RECURSIVE_METHOD,            // Recursive method calls
        HIGH_CYCLOMATIC_COMPLEXITY,  // Complex control flow
        COLLECTION_OPERATIONS,       // Heavy collection processing
        IO_OPERATIONS,               // File or network I/O
        DATABASE_ACCESS,             // JPA/JDBC operations
        CACHING_OPERATIONS,          // Cache-related operations
        MANUALLY_SPECIFIED,          // User added to whitelist
        SPRING_BEAN_METHOD,          // Method from a Spring bean
        TRANSACTIONAL_METHOD,        // @Transactional annotated
        ASYNC_METHOD,                // @Async annotated
        SCHEDULED_METHOD             // @Scheduled annotated
    }

    @Data
    @Builder
    public static class ComplexityMetrics implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private int cyclomaticComplexity;
        private int linesOfCode;
        private int numberOfLoops;
        private int numberOfBranches;
        private int nestingDepth;
    }

    public void addReason(HotspotReason reason) {
        if (reasons == null) {
            reasons = new HashSet<>();
        }
        reasons.add(reason);
        recalculatePriority();
    }

    private void recalculatePriority() {
        int score = 0;
        for (HotspotReason reason : reasons) {
            score += getReasonWeight(reason);
        }
        this.priorityScore = score;
    }

    private int getReasonWeight(HotspotReason reason) {
        return switch (reason) {
            case DATABASE_ACCESS, CONTAINS_SQL_QUERY -> 10;
            case IO_OPERATIONS -> 9;
            case CONTROLLER_ENDPOINT -> 8;
            case HIGH_CYCLOMATIC_COMPLEXITY -> 7;
            case RECURSIVE_METHOD -> 7;
            case CONTAINS_LOOP, COLLECTION_OPERATIONS -> 6;
            case TRANSACTIONAL_METHOD -> 5;
            case SERVICE_ANNOTATION, REPOSITORY_ANNOTATION -> 4;
            case CACHING_OPERATIONS -> 4;
            case ASYNC_METHOD, SCHEDULED_METHOD -> 3;
            case SPRING_BEAN_METHOD -> 2;
            case MANUALLY_SPECIFIED -> 10;
        };
    }
}