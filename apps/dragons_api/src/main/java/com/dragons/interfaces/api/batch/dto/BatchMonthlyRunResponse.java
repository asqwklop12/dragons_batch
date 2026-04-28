package com.dragons.interfaces.api.batch.dto;

public record BatchMonthlyRunResponse(
    long jobExecutionId,
    String status,
    String startTime,
    String endTime,
    String itemCategoryCode,
    String year,
    String month,
    boolean mockMode
) {
}
