package com.application;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class BatchDailySchedulerTest {

  @Test
  void scheduleDailyPriceFetchRunsPreviousDayBatchWithConfiguredCategory() {
    BatchManualRunService batchManualRunService = org.mockito.Mockito.mock(BatchManualRunService.class);
    Clock clock = Clock.fixed(Instant.parse("2026-04-14T03:01:00Z"), ZoneId.of("Asia/Seoul"));
    BatchDailyScheduler scheduler = new BatchDailyScheduler(batchManualRunService, clock, "500");

    given(batchManualRunService.run("500", LocalDate.of(2026, 4, 13)))
        .willReturn(new BatchRunResult(
            31L,
            "COMPLETED",
            LocalDateTime.of(2026, 4, 14, 12, 1),
            LocalDateTime.of(2026, 4, 14, 12, 1, 5),
            12
        ));

    scheduler.scheduleDailyPriceFetch();

    then(batchManualRunService).should().run("500", LocalDate.of(2026, 4, 13));
  }
}
