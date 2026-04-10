package com.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.jdbc.core.JdbcTemplate;

class BatchManualRunServiceTest {

  @Test
  void runLaunchesJobWithRequestParametersAndReturnsExecutionSummary() throws Exception {
    JobOperator jobOperator = mock(JobOperator.class);
    Job kamisPriceJob = mock(Job.class);
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    JobExecution jobExecution = mock(JobExecution.class);
    StepExecution stepExecution = mock(StepExecution.class);
    BatchManualRunService batchManualRunService = new BatchManualRunService(jobOperator, kamisPriceJob, jdbcTemplate);
    LocalDate regDay = LocalDate.of(2024, 1, 15);

    given(jobOperator.start(eq(kamisPriceJob), org.mockito.ArgumentMatchers.any(JobParameters.class)))
        .willReturn(jobExecution);
    given(jobExecution.getStatus()).willReturn(BatchStatus.COMPLETED);
    given(jobExecution.getId()).willReturn(31L);
    given(jobExecution.getStartTime()).willReturn(LocalDateTime.of(2024, 1, 15, 10, 0));
    given(jobExecution.getEndTime()).willReturn(LocalDateTime.of(2024, 1, 15, 10, 1));
    given(jobExecution.getStepExecutions()).willReturn(Set.of(stepExecution));
    given(stepExecution.getWriteCount()).willReturn(2L);

    BatchRunResult result = batchManualRunService.run("200", regDay);

    ArgumentCaptor<JobParameters> jobParametersCaptor = ArgumentCaptor.forClass(JobParameters.class);
    then(jobOperator).should().start(eq(kamisPriceJob), jobParametersCaptor.capture());

    JobParameters jobParameters = jobParametersCaptor.getValue();
    assertThat(jobParameters.getString("itemCategoryCode")).isEqualTo("200");
    assertThat(jobParameters.getString("regDay")).isEqualTo("2024-01-15");
    assertThat(jobParameters.getLong("requestedAt")).isNotNull();

    assertThat(result.jobExecutionId()).isEqualTo(31L);
    assertThat(result.status()).isEqualTo("COMPLETED");
    assertThat(result.startTime()).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 0));
    assertThat(result.endTime()).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 1));
    assertThat(result.writeCount()).isEqualTo(2);
  }

  @Test
  void runThrowsWhenBatchExecutionFails() throws Exception {
    JobOperator jobOperator = mock(JobOperator.class);
    Job kamisPriceJob = mock(Job.class);
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    JobExecution jobExecution = mock(JobExecution.class);
    BatchManualRunService batchManualRunService = new BatchManualRunService(jobOperator, kamisPriceJob, jdbcTemplate);
    RuntimeException failure = new RuntimeException("writer failed");

    given(jobOperator.start(eq(kamisPriceJob), org.mockito.ArgumentMatchers.any(JobParameters.class)))
        .willReturn(jobExecution);
    given(jobExecution.getStatus()).willReturn(BatchStatus.FAILED);
    given(jobExecution.getAllFailureExceptions()).willReturn(List.of(failure));

    assertThatThrownBy(() -> batchManualRunService.run("200", LocalDate.of(2024, 1, 15)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("배치 실행에 실패했습니다.")
        .hasRootCause(failure);
  }
}
