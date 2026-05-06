package com.config;

import com.process.SkippablePriceDataException;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;

public class BatchSkipPolicy implements SkipPolicy {

  private final long skipLimit;

  public BatchSkipPolicy(long skipLimit) {
    this.skipLimit = skipLimit;
  }

  @Override
  public boolean shouldSkip(Throwable throwable, long skipCount) {
    if (!(throwable instanceof SkippablePriceDataException)) {
      return false;
    }

    if (skipCount >= skipLimit) {
      throw new SkipLimitExceededException(skipLimit, throwable);
    }

    return true;
  }
}
