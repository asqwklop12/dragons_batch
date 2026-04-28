package com.dragons.interfaces.api.batch.dto;

import java.util.Map;

public record BatchStatusItemResponse(
    long jobInstanceId,
    long jobExecutionId,
    String jobName,
    String status,
    String startTime,
    String endTime,
    String exitCode,
    Map<String, String> params
) {
}
