package com.application;

import java.time.LocalDateTime;
import java.util.Map;

public record BatchStatusItemResult(
    long jobInstanceId,
    String jobName,
    long jobExecutionId,
    String status,
    LocalDateTime startTime,
    LocalDateTime endTime,
    String exitCode,
    Map<String, String> params
) {

  public BatchStatusItemResult {
    params = params == null ? Map.of() : Map.copyOf(params);
  }
}
