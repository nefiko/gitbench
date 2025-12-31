package model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ComplexityMetrics {
    private int cyclomaticComplexity;
    private int linesOfCode;
    private int numberOfLoops;
    private int numberOfBranches;
    private int nestingDepth;
}