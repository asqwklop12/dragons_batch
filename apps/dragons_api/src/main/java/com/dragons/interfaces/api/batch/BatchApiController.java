package com.dragons.interfaces.api.batch;

import com.application.BatchManualRunService;
import com.application.BatchMonthlyRunService;
import com.application.BatchRunResult;
import com.application.BatchStatusResult;
import com.dragons.interfaces.api.batch.dto.BatchConfigResponse;
import com.dragons.interfaces.api.batch.dto.BatchMonthlyRunRequest;
import com.dragons.interfaces.api.batch.dto.BatchMonthlyRunResponse;
import com.dragons.interfaces.api.batch.dto.BatchRunRequest;
import com.dragons.interfaces.api.batch.dto.BatchRunResponse;
import com.dragons.interfaces.api.batch.dto.BatchStatusItemResponse;
import com.dragons.interfaces.api.batch.dto.BatchStatusRequest;
import com.dragons.interfaces.api.batch.dto.BatchStatusResponse;
import com.dragons.support.ApiResponse;
import com.properties.MarketPriceApiProperties;
import constant.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/batch")
@Tag(name = "Batch", description = "배치 실행 및 상태 조회 API")
@RequiredArgsConstructor
public class BatchApiController {

  private final BatchMonthlyRunService batchMonthlyRunService;
  private final BatchManualRunService batchManualRunService;
  private final MarketPriceApiProperties marketPriceApiProperties;


  @PostMapping("/run")
  @Operation(summary = "배치 수동 실행")
  public ApiResponse<BatchRunResponse> runBatch(@ModelAttribute BatchRunRequest request) {
    String itemCategoryCode = resolveItemCategoryCode(request);
    String targetRegDay = resolveRegDay(request);
    BatchRunResult result = batchManualRunService.run(itemCategoryCode, LocalDate.parse(targetRegDay));

    return ApiResponse.successResponse(
        new BatchRunResponse(
            result.jobExecutionId(),
            result.status(),
            formatDateTime(result.startTime()),
            formatDateTime(result.endTime()),
            itemCategoryCode,
            targetRegDay,
            marketPriceApiProperties.isMockMode()
    ));
  }

  @PostMapping("/run-monthly")
  @Operation(summary = "월별 배치 수동 실행")
  public ApiResponse<BatchMonthlyRunResponse> runMonthlyBatch(@Valid @ModelAttribute BatchMonthlyRunRequest request) {
    String itemCategoryCode = resolveItemCategoryCode(request.itemCategoryCode());
    YearMonth targetYearMonth = resolveYearMonth(request.year(), request.month());
    BatchRunResult result = batchMonthlyRunService.run(itemCategoryCode, targetYearMonth);

    return ApiResponse.successResponse(
        new BatchMonthlyRunResponse(
            result.jobExecutionId(),
            result.status(),
            formatDateTime(result.startTime()),
            formatDateTime(result.endTime()),
            itemCategoryCode,
            String.valueOf(targetYearMonth.getYear()),
            "%02d".formatted(targetYearMonth.getMonthValue()),
            marketPriceApiProperties.isMockMode()
        )
    );
  }

  @GetMapping("/status")
  @Operation(summary = "배치 실행 이력 조회")
  public ApiResponse<BatchStatusResponse> getBatchStatus(@ModelAttribute BatchStatusRequest request) {
    BatchStatusResult result = batchManualRunService.latestStatuses(request.resolvedLimit());
    boolean certKeySet = hasConcreteValue(marketPriceApiProperties.getCertKey());
    boolean certIdSet = hasConcreteValue(marketPriceApiProperties.getCertId());
    boolean mockMode = marketPriceApiProperties.isMockMode();

    return ApiResponse.successResponse(
        new BatchStatusResponse(
            result.count(),
            mockMode,
            certKeySet && certIdSet && !mockMode,
            result.items().stream()
                .map(item -> new BatchStatusItemResponse(
                    item.jobInstanceId(),
                    item.jobExecutionId(),
                    item.jobName(),
                    item.status(),
                    formatDateTime(item.startTime()),
                    formatDateTime(item.endTime()),
                    item.exitCode(),
                    item.params()
                ))
                .toList()
        )
    );
  }

  @GetMapping("/config")
  @Operation(summary = "API 설정 확인")
  public ApiResponse<BatchConfigResponse> getBatchConfig() {
    boolean certKeySet = hasConcreteValue(marketPriceApiProperties.getCertKey());
    boolean certIdSet = hasConcreteValue(marketPriceApiProperties.getCertId());
    boolean mockMode = marketPriceApiProperties.isMockMode();

    return ApiResponse.successResponse(
        new BatchConfigResponse(
            certKeySet && certIdSet && !mockMode,
            mockMode,
            marketPriceApiProperties.getBaseUrl(),
            certKeySet,
            certIdSet
        )
    );
  }

  private String resolveItemCategoryCode(BatchRunRequest request) {
    return resolveItemCategoryCode(request.itemCategoryCode());
  }

  private String resolveItemCategoryCode(String itemCategoryCode) {
    return itemCategoryCode == null || itemCategoryCode.isBlank() ? "200" : itemCategoryCode;
  }

  private String resolveRegDay(BatchRunRequest request) {
    return request.regDay() == null || request.regDay().isBlank()
        ? LocalDate.now().toString()
        : request.regDay();
  }

  private YearMonth resolveYearMonth(String year, String month) {
    boolean yearProvided = year != null && !year.isBlank();
    boolean monthProvided = month != null && !month.isBlank();

    if (!yearProvided || !monthProvided) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "year와 month는 함께 입력해야 합니다.");
    }

    try {
      return YearMonth.of(Integer.parseInt(year), Integer.parseInt(month));
    } catch (NumberFormatException | DateTimeException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "year/month 형식이 올바르지 않습니다.", exception);
    }
  }

  private String formatDateTime(java.time.LocalDateTime value) {
    return value == null ? null : Constants.DATE_TIME_FORMATTER.format(value);
  }

  private boolean hasConcreteValue(String value) {
    return value != null && !value.isBlank() && !value.startsWith("${");
  }
}
