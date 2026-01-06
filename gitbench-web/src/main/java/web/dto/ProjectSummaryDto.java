package web.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class ProjectSummaryDto {
    private String name;
    private boolean configured;
    private int regressions;
    private int improvements;
    private Instant lastRunTimestamp;
    private String lastCommitHash;
}