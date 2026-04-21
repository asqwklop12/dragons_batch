package com.dragons.interfaces.api.batch.dto;

public record BatchMonthlyRunRequest(
    String itemCategoryCode,
    String yyyy,
    String mm
) {
}
