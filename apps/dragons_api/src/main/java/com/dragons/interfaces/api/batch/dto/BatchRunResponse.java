package com.dragons.interfaces.api.batch.dto;

public record BatchRunResponse(
    long jobId,
    String status,
    String startTime,
    String endTime,
    String itemCategoryCode,
    String regDay,
    boolean mockMode
) {
}
