package com.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.StepExecution;

class BatchMonthlyRunServiceTest {

  @Test
  void runLaunchesMonthlyJobWithRequestParametersAndReturnsExecutionSummary() throws Exception {
    JobOperator jobOperator = mock(JobOperator.class);
    Job kamisMonthlyPriceJob = mock(Job.class);
    Clock clock = Clock.fixed(Instant.parse("2026-04-21T00:00:00Z"), ZoneOffset.UTC);
    JobExecution jobExecution = mock(JobExecution.class);
    StepExecution stepExecution = mock(StepExecution.class);
    BatchMonthlyRunService batchMonthlyRunService = new BatchMonthlyRunService(jobOperator, kamisMonthlyPriceJob, clock);
    YearMonth yearMonth = YearMonth.of(2024, 1);

    given(jobOperator.start(eq(kamisMonthlyPriceJob), org.mockito.ArgumentMatchers.any(JobParameters.class)))
        .willReturn(jobExecution);
    given(jobExecution.getStatus()).willReturn(BatchStatus.COMPLETED);
    given(jobExecution.getId()).willReturn(41L);
    given(jobExecution.getStartTime()).willReturn(LocalDateTime.of(2024, 1, 31, 10, 0));
    given(jobExecution.getEndTime()).willReturn(LocalDateTime.of(2024, 1, 31, 10, 1));
    given(jobExecution.getStepExecutions()).willReturn(Set.of(stepExecution));
    given(stepExecution.getWriteCount()).willReturn(4L);

    BatchRunResult result = batchMonthlyRunService.run("200", yearMonth);

    ArgumentCaptor<JobParameters> jobParametersCaptor = ArgumentCaptor.forClass(JobParameters.class);
    then(jobOperator).should().start(eq(kamisMonthlyPriceJob), jobParametersCaptor.capture());

    JobParameters jobParameters = jobParametersCaptor.getValue();
    assertThat(jobParameters.getString("itemCategoryCode")).isEqualTo("200");
    assertThat(jobParameters.getString("yyyy")).isEqualTo("2024");
    assertThat(jobParameters.getString("mm")).isEqualTo("01");
    assertThat(jobParameters.getLong("requestedAt")).isEqualTo(clock.millis());

    assertThat(result.jobExecutionId()).isEqualTo(41L);
    assertThat(result.status()).isEqualTo("COMPLETED");
    assertThat(result.writeCount()).isEqualTo(4);
  }

  @Test
  void runThrowsWhenMonthlyBatchExecutionFails() throws Exception {
    JobOperator jobOperator = mock(JobOperator.class);
    Job kamisMonthlyPriceJob = mock(Job.class);
    Clock clock = Clock.fixed(Instant.parse("2026-04-21T00:00:00Z"), ZoneOffset.UTC);
    JobExecution jobExecution = mock(JobExecution.class);
    BatchMonthlyRunService batchMonthlyRunService = new BatchMonthlyRunService(jobOperator, kamisMonthlyPriceJob, clock);
    RuntimeException failure = new RuntimeException("monthly writer failed");

    given(jobOperator.start(eq(kamisMonthlyPriceJob), org.mockito.ArgumentMatchers.any(JobParameters.class)))
        .willReturn(jobExecution);
    given(jobExecution.getStatus()).willReturn(BatchStatus.FAILED);
    given(jobExecution.getAllFailureExceptions()).willReturn(List.of(failure));

    assertThatThrownBy(() -> batchMonthlyRunService.run("200", YearMonth.of(2024, 1)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("월별 배치 실행에 실패했습니다.")
        .hasRootCause(failure);
  }
}
