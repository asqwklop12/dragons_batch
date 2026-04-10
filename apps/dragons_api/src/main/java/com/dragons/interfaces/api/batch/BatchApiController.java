package com.dragons.interfaces.api.batch;

import com.application.BatchManualRunService;
import com.application.BatchRunResult;
import com.application.BatchStatusResult;
import com.dragons.interfaces.api.batch.dto.BatchConfigResponse;
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
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/batch")
@Tag(name = "Batch", description = "배치 실행 및 상태 조회 API")
@RequiredArgsConstructor
public class BatchApiController {

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
            false
    ));
  }

  @GetMapping("/status")
  @Operation(summary = "배치 실행 이력 조회")
  public ApiResponse<BatchStatusResponse> getBatchStatus(@ModelAttribute BatchStatusRequest request) {
    BatchStatusResult result = batchManualRunService.latestStatuses(request.resolvedLimit());
    return ApiResponse.successResponse(
        new BatchStatusResponse(
            result.count(),
            false,
            false,
            result.items().stream()
                .map(item -> new BatchStatusItemResponse(
                    item.jobInstanceId(),
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
    return request.itemCategoryCode() == null || request.itemCategoryCode().isBlank()
        ? "200"
        : request.itemCategoryCode();
  }

  private String resolveRegDay(BatchRunRequest request) {
    return request.regDay() == null || request.regDay().isBlank()
        ? LocalDate.now().toString()
        : request.regDay();
  }

  private String formatDateTime(java.time.LocalDateTime value) {
    return value == null ? null : Constants.DATE_TIME_FORMATTER.format(value);
  }

  private boolean hasConcreteValue(String value) {
    return value != null && !value.isBlank() && !value.startsWith("${");
  }
}
