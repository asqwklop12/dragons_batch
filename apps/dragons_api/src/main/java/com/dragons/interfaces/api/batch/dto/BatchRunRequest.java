package com.dragons.interfaces.api.batch.dto;

public record BatchRunRequest(
    String itemCategoryCode,
    String regDay
) {
}
