package com.config;

import lombok.extern.slf4j.Slf4j;
import model.price.PriceData;
import model.price.PriceReadItem;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.ItemWriteListener;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.infrastructure.item.Chunk;

@Slf4j
public class BatchStepMonitoringListener
    implements ItemWriteListener<PriceData>, SkipListener<PriceReadItem, PriceData>, StepExecutionListener {

  @Override
  public void beforeWrite(Chunk<? extends PriceData> items) {
    log.debug("About to write {} price items.", items.size());
  }

  @Override
  public void afterWrite(Chunk<? extends PriceData> items) {
    log.debug("Successfully wrote {} price items.", items.size());
  }

  @Override
  public void onWriteError(Exception exception, Chunk<? extends PriceData> items) {
    log.error("Write failed for {} price items.", items.size(), exception);
  }

  @Override
  public void onSkipInProcess(PriceReadItem item, Throwable throwable) {
    log.warn(
        "Skipped invalid price item during process. itemCode={}, itemName={}, regDay={}, reason={}",
        item == null ? null : item.itemCode(),
        item == null ? null : item.itemName(),
        item == null ? null : item.regDay(),
        throwable.getMessage()
    );
  }

  @Override
  public void onSkipInWrite(PriceData item, Throwable throwable) {
    log.warn(
        "Skipped price data during write. itemCode={}, itemName={}, regDay={}, reason={}",
        item == null ? null : item.getItemCode(),
        item == null ? null : item.getItemName(),
        item == null ? null : item.getRegDay(),
        throwable.getMessage()
    );
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    long totalSkipCount =
        stepExecution.getReadSkipCount() + stepExecution.getProcessSkipCount() + stepExecution.getWriteSkipCount();

    if (totalSkipCount > 0) {
      log.warn(
          "Step completed with skipped items. stepName={}, status={}, readSkip={}, processSkip={}, writeSkip={}, totalSkip={}",
          stepExecution.getStepName(),
          stepExecution.getStatus(),
          stepExecution.getReadSkipCount(),
          stepExecution.getProcessSkipCount(),
          stepExecution.getWriteSkipCount(),
          totalSkipCount
      );
    } else {
      log.info(
          "Step completed without skips. stepName={}, status={}, readCount={}, writeCount={}",
          stepExecution.getStepName(),
          stepExecution.getStatus(),
          stepExecution.getReadCount(),
          stepExecution.getWriteCount()
      );
    }

    boolean skipLimitExceeded = stepExecution.getFailureExceptions().stream()
        .anyMatch(exception -> exception instanceof SkipLimitExceededException);
    if (skipLimitExceeded) {
      log.error(
          "Skip limit exceeded. stepName={}, skipCount={}, failureCount={}",
          stepExecution.getStepName(),
          totalSkipCount,
          stepExecution.getFailureExceptions().size()
      );
    }

    return null;
  }
}
