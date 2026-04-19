package com.application;

import java.time.Clock;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BatchDailyScheduler {

  private final BatchManualRunService batchManualRunService;
  private final Clock clock;
  private final String itemCategoryCode;

  public BatchDailyScheduler(
      BatchManualRunService batchManualRunService,
      Clock clock,
      @Value("${batch.schedule.daily.item-category-code:200}") String itemCategoryCode
  ) {
    this.batchManualRunService = batchManualRunService;
    this.clock = clock;
    this.itemCategoryCode = itemCategoryCode;
  }

  @Scheduled(
      cron = "${batch.schedule.daily.cron:0 1 0 * * *}",
      zone = "${batch.schedule.daily.zone:Asia/Seoul}"
  )
  public void scheduleDailyPriceFetch() {
    LocalDate targetRegDay = LocalDate.now(clock).minusDays(1);
    BatchRunResult result = batchManualRunService.run(itemCategoryCode, targetRegDay);
    log.info(
        "Scheduled daily batch completed. itemCategoryCode={}, regDay={}, jobExecutionId={}, status={}",
        itemCategoryCode,
        targetRegDay,
        result.jobExecutionId(),
        result.status()
    );
  }
}
