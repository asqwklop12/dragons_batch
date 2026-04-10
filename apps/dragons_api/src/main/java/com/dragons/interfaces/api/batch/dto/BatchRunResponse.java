package com.dragons.interfaces.api.batch.dto;

public record BatchRunResponse(
    long jobExecutionId,
    String status,
    String startTime,
    String endTime,
    String itemCategoryCode,
    String regDay,
    boolean mockMode
) {
}
