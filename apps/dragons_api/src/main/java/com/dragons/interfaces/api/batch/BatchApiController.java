package com.dragons.interfaces.api.batch;

import com.dragons.interfaces.api.batch.dto.BatchConfigResponse;
import com.dragons.interfaces.api.batch.dto.BatchRunRequest;
import com.dragons.interfaces.api.batch.dto.BatchRunResponse;
import com.dragons.interfaces.api.batch.dto.BatchStatusItemResponse;
import com.dragons.interfaces.api.batch.dto.BatchStatusResponse;
import com.dragons.support.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/batch")
@Tag(name = "Batch", description = "배치 실행 및 상태 조회 API")
public class BatchApiController {

  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  private static final String JOB_NAME = "kamisPriceJob";

  private final AtomicLong jobSequence = new AtomicLong(3);
  private final CopyOnWriteArrayList<BatchStatusItemResponse> batchHistories = new CopyOnWriteArrayList<>(List.of(
      new BatchStatusItemResponse(
          2L,
          JOB_NAME,
          "COMPLETED",
          "2024-01-15T10:35:00",
          "2024-01-15T10:35:03",
          "COMPLETED",
          Map.of(
              "itemCategoryCode", "200",
              "regDay", "2024-01-15"
          )
      ),
      new BatchStatusItemResponse(
          1L,
          JOB_NAME,
          "COMPLETED",
          "2024-01-14T10:30:00",
          "2024-01-14T10:30:04",
          "COMPLETED",
          Map.of(
              "itemCategoryCode", "100",
              "regDay", "2024-01-14"
          )
      )
  ));

  @PostMapping("/run")
  @Operation(summary = "배치 수동 실행")
  public ApiResponse<BatchRunResponse> runBatch(@ModelAttribute BatchRunRequest request) {
    String itemCategoryCode = resolveItemCategoryCode(request);
    String targetRegDay = resolveRegDay(request);
    LocalDateTime startTime = LocalDateTime.now();
    LocalDateTime endTime = startTime.plusSeconds(5);
    long jobId = jobSequence.getAndIncrement();

    batchHistories.add(
        0,
        new BatchStatusItemResponse(
            jobId,
            JOB_NAME,
            "COMPLETED",
            DATE_TIME_FORMATTER.format(startTime),
            DATE_TIME_FORMATTER.format(endTime),
            "COMPLETED",
            Map.of(
                "itemCategoryCode", itemCategoryCode,
                "regDay", targetRegDay
            )
        )
    );

    return new ApiResponse<>(
        true,
        new BatchRunResponse(
            jobId,
            "COMPLETED",
            DATE_TIME_FORMATTER.format(startTime),
            DATE_TIME_FORMATTER.format(endTime),
            itemCategoryCode,
            targetRegDay,
            true
        ),
        "배치 실행 완료"
    );
  }

  @GetMapping("/status")
  @Operation(summary = "배치 실행 이력 조회")
  public ApiResponse<BatchStatusResponse> getBatchStatus() {
    return ApiResponse.successResponse(
        new BatchStatusResponse(
            batchHistories.size(),
            true,
            false,
            List.copyOf(batchHistories)
        )
    );
  }

  @GetMapping("/config")
  @Operation(summary = "API 설정 확인")
  public ApiResponse<BatchConfigResponse> getBatchConfig() {
    return ApiResponse.successResponse(
        new BatchConfigResponse(
            false,
            true,
            "https://www.kamis.or.kr/service/price/xml.do",
            false,
            false
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
}
