package com.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BatchManualRunService {

  private final JobOperator jobOperator;
  private final Job kamisPriceJob;
  private final JdbcTemplate jdbcTemplate;
  private final Clock clock;

  public BatchRunResult run(String itemCategoryCode, LocalDate regDay) {
    try {
      JobExecution jobExecution = jobOperator.start(
          kamisPriceJob,
          new JobParametersBuilder()
              .addString("itemCategoryCode", itemCategoryCode)
              .addString("regDay", regDay.toString())
              .addLong("requestedAt", clock.millis())
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

  public BatchStatusResult latestStatuses(int limit) {
    List<BatchExecutionSummary> executions = jdbcTemplate.query(
        """
            select
              ji.JOB_INSTANCE_ID,
              ji.JOB_NAME,
              je.JOB_EXECUTION_ID,
              je.STATUS,
              je.START_TIME,
              je.END_TIME,
              je.EXIT_CODE
            from BATCH_JOB_EXECUTION je
            join BATCH_JOB_INSTANCE ji on ji.JOB_INSTANCE_ID = je.JOB_INSTANCE_ID
            order by je.JOB_EXECUTION_ID desc
            limit ?
            """,
        (resultSet, rowNum) -> new BatchExecutionSummary(
            resultSet.getLong("JOB_INSTANCE_ID"),
            resultSet.getString("JOB_NAME"),
            resultSet.getLong("JOB_EXECUTION_ID"),
            resultSet.getString("STATUS"),
            toLocalDateTime(resultSet.getTimestamp("START_TIME")),
            toLocalDateTime(resultSet.getTimestamp("END_TIME")),
            resultSet.getString("EXIT_CODE")
        ),
        limit
    );

    if (executions.isEmpty()) {
      return new BatchStatusResult(0, List.of());
    }

    Map<Long, Map<String, String>> paramsByExecutionId = loadParamsBatch(
        executions.stream()
            .map(BatchExecutionSummary::jobExecutionId)
            .toList()
    );

    List<BatchStatusItemResult> items = executions.stream()
        .map(execution -> new BatchStatusItemResult(
            execution.jobInstanceId(),
            execution.jobName(),
            execution.jobExecutionId(),
            execution.status(),
            execution.startTime(),
            execution.endTime(),
            execution.exitCode(),
            paramsByExecutionId.getOrDefault(execution.jobExecutionId(), Map.of())
        ))
        .toList();

    return new BatchStatusResult(items.size(), items);
  }

  private Map<Long, Map<String, String>> loadParamsBatch(List<Long> jobExecutionIds) {
    String placeholders = String.join(", ", jobExecutionIds.stream().map(ignored -> "?").toList());
    List<BatchExecutionParam> params = jdbcTemplate.query(
        """
            select JOB_EXECUTION_ID, PARAMETER_NAME, PARAMETER_VALUE
            from BATCH_JOB_EXECUTION_PARAMS
            where JOB_EXECUTION_ID in (%s)
              and PARAMETER_NAME <> 'requestedAt'
            order by JOB_EXECUTION_ID asc, PARAMETER_NAME asc
            """.formatted(placeholders),
        (resultSet, rowNum) -> new BatchExecutionParam(
            resultSet.getLong("JOB_EXECUTION_ID"),
            resultSet.getString("PARAMETER_NAME"),
            resultSet.getString("PARAMETER_VALUE")
        ),
        jobExecutionIds.toArray()
    );

    Map<Long, Map<String, String>> paramsByExecutionId = new LinkedHashMap<>();
    for (BatchExecutionParam param : params) {
      paramsByExecutionId.computeIfAbsent(param.jobExecutionId(), ignored -> new LinkedHashMap<>())
          .put(param.parameterName(), param.parameterValue());
    }
    return paramsByExecutionId;
  }

  private LocalDateTime toLocalDateTime(java.sql.Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toLocalDateTime();
  }

  private record BatchExecutionSummary(
      long jobInstanceId,
      String jobName,
      long jobExecutionId,
      String status,
      LocalDateTime startTime,
      LocalDateTime endTime,
      String exitCode
  ) {
  }

  private record BatchExecutionParam(
      long jobExecutionId,
      String parameterName,
      String parameterValue
  ) {
  }
}
