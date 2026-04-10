package com.application;

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

  public BatchStatusResult latestStatuses(int limit) {
    List<BatchStatusItemResult> items = jdbcTemplate.query(
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
        (resultSet, rowNum) -> new BatchStatusItemResult(
            resultSet.getLong("JOB_INSTANCE_ID"),
            resultSet.getString("JOB_NAME"),
            resultSet.getString("STATUS"),
            toLocalDateTime(resultSet.getTimestamp("START_TIME")),
            toLocalDateTime(resultSet.getTimestamp("END_TIME")),
            resultSet.getString("EXIT_CODE"),
            loadParams(resultSet.getLong("JOB_EXECUTION_ID"))
        ),
        limit
    );
    return new BatchStatusResult(items.size(), items);
  }

  private Map<String, String> loadParams(long jobExecutionId) {
    List<Map.Entry<String, String>> entries = jdbcTemplate.query(
        """
            select PARAMETER_NAME, PARAMETER_VALUE
            from BATCH_JOB_EXECUTION_PARAMS
            where JOB_EXECUTION_ID = ?
            order by PARAMETER_NAME asc
            """,
        (resultSet, rowNum) -> Map.entry(
            resultSet.getString("PARAMETER_NAME"),
            resultSet.getString("PARAMETER_VALUE")
        ),
        jobExecutionId
    );

    Map<String, String> params = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : entries) {
      if ("requestedAt".equals(entry.getKey())) {
        continue;
      }
      params.put(entry.getKey(), entry.getValue());
    }
    return params;
  }

  private LocalDateTime toLocalDateTime(java.sql.Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toLocalDateTime();
  }
}
