package com.application;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BatchManualRunService {

  private final JobOperator jobOperator;
  private final Job kamisPriceJob;

  public BatchRunResult run(String itemCategoryCode, LocalDate regDay) {
    try {
      JobExecution jobExecution = jobOperator.start(
          kamisPriceJob,
          new JobParametersBuilder()
              .addString("itemCategoryCode", itemCategoryCode)
              .addString("regDay", regDay.toString())
              .addLong("requestedAt", System.currentTimeMillis())
              .toJobParameters()
      );

      if (jobExecution.getStatus() != BatchStatus.COMPLETED) {
        throw new IllegalStateException(
            "배치 실행에 실패했습니다.",
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
      throw new IllegalStateException("배치 실행에 실패했습니다.", exception);
    }
  }
}
