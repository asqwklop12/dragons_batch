package com.application;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BatchStatusQueryService {

  private final JdbcTemplate jdbcTemplate;

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
        (resultSet, rowNum) -> mapStatusItem(resultSet),
        limit
    );
    return new BatchStatusResult(items.size(), items);
  }

  private BatchStatusItemResult mapStatusItem(ResultSet resultSet) throws SQLException {
    long jobExecutionId = resultSet.getLong("JOB_EXECUTION_ID");
    return new BatchStatusItemResult(
        resultSet.getLong("JOB_INSTANCE_ID"),
        resultSet.getString("JOB_NAME"),
        resultSet.getString("STATUS"),
        toLocalDateTime(resultSet.getTimestamp("START_TIME")),
        toLocalDateTime(resultSet.getTimestamp("END_TIME")),
        resultSet.getString("EXIT_CODE"),
        loadParams(jobExecutionId)
    );
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

  private LocalDateTime toLocalDateTime(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toLocalDateTime();
  }
}
