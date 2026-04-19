package com.application;

import java.time.LocalDateTime;

public record BatchRunResult(
    long jobExecutionId,
    String status,
    LocalDateTime startTime,
    LocalDateTime endTime,
    long writeCount
) {
}
