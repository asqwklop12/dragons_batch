package com.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.exception.SkippablePriceDataException;
import constant.Constants;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;

class BatchSkipPolicyTest {

  @Test
  void shouldSkipReturnsTrueForSkippableExceptionUnderLimit() {
    BatchSkipPolicy batchSkipPolicy = new BatchSkipPolicy(Constants.SKIP_LIMIT);

    assertThat(batchSkipPolicy.shouldSkip(new SkippablePriceDataException("bad row"), 0)).isTrue();
    assertThat(batchSkipPolicy.shouldSkip(new SkippablePriceDataException("bad row"), Constants.SKIP_LIMIT - 1)).isTrue();
  }

  @Test
  void shouldSkipThrowsWhenSkippableExceptionExceedsLimit() {
    BatchSkipPolicy batchSkipPolicy = new BatchSkipPolicy(Constants.SKIP_LIMIT);

    assertThatThrownBy(() -> batchSkipPolicy.shouldSkip(new SkippablePriceDataException("bad row"), Constants.SKIP_LIMIT))
        .isInstanceOf(SkipLimitExceededException.class);
  }

  @Test
  void shouldSkipReturnsFalseForNonSkippableException() {
    BatchSkipPolicy batchSkipPolicy = new BatchSkipPolicy(Constants.SKIP_LIMIT);

    assertThat(batchSkipPolicy.shouldSkip(new IllegalStateException("db down"), 0)).isFalse();
  }
}
