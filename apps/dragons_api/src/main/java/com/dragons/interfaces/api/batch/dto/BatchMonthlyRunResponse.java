package com.dragons.interfaces.api.batch.dto;

public record BatchMonthlyRunResponse(
    long jobExecutionId,
    String status,
    String startTime,
    String endTime,
    String itemCategoryCode,
    String yyyy,
    String mm,
    boolean mockMode
) {
}
