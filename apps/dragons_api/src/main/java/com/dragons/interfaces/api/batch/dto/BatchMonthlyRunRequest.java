package com.dragons.interfaces.api.batch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BatchMonthlyRunRequest(
    @NotBlank(message = "itemCategoryCode는 필수입니다.") String itemCategoryCode,
    @NotBlank(message = "year는 필수입니다.")
    @Pattern(regexp = "\\d{4}", message = "year는 4자리 연도여야 합니다.")
    String year,
    @NotBlank(message = "month는 필수입니다.")
    @Pattern(regexp = "(0[1-9]|1[0-2])", message = "month는 01~12 형식이어야 합니다.")
    String month
) {
}
