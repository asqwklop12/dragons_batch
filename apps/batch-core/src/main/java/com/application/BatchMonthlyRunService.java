package com.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class BatchMonthlyRunService {

  private final JobOperator jobOperator;
  private final Job kamisMonthlyPriceJob;
  private final Clock clock;

  public BatchMonthlyRunService(
      JobOperator jobOperator,
      @Qualifier("kamisMonthlyPriceJob") Job kamisMonthlyPriceJob,
      Clock clock
  ) {
    this.jobOperator = jobOperator;
    this.kamisMonthlyPriceJob = kamisMonthlyPriceJob;
    this.clock = clock;
  }

  public BatchRunResult run(String itemCategoryCode, YearMonth yearMonth) {
    try {
      JobExecution jobExecution = jobOperator.start(
          kamisMonthlyPriceJob,
          new JobParametersBuilder()
              .addString("itemCategoryCode", itemCategoryCode)
              .addString("yyyy", String.valueOf(yearMonth.getYear()))
              .addString("mm", "%02d".formatted(yearMonth.getMonthValue()))
              .addLong("requestedAt", clock.millis())
              .toJobParameters()
      );

      if (jobExecution.getStatus() != BatchStatus.COMPLETED) {
        throw new IllegalStateException(
            "월별 배치 실행에 실패했습니다.",
            jobExecution.getAllFailureExceptions().stream().findFirst().orElse(null)
        );
      }

      return new BatchRunResult(
          jobExecution.getId(),
          jobExecution.getStatus().name(),
          jobExecution.getStartTime(),
          jobExecution.getEndTime(),
          jobExecution.getStepExecutions().stream()
              .mapToLong(StepExecution::getWriteCount)
              .sum()
      );
    } catch (IllegalStateException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new IllegalStateException("월별 배치 실행에 실패했습니다.", exception);
    }
  }
}
