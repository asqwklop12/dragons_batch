package com.application;

import java.time.LocalDateTime;
import java.util.Map;

public record BatchStatusItemResult(
    long jobInstanceId,
    String jobName,
    String status,
    LocalDateTime startTime,
    LocalDateTime endTime,
    String exitCode,
    Map<String, String> params
) {
}
