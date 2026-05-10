package com.application;

import java.lang.reflect.Method;

final class TransientPriceReadExceptionMapper {

  private static final String FEIGN_RETRYABLE_EXCEPTION = "feign.RetryableException";
  private static final String FEIGN_EXCEPTION_PREFIX = "feign.FeignException";

  private TransientPriceReadExceptionMapper() {
  }

  static boolean isRetryable(Throwable throwable) {
    String className = throwable.getClass().getName();
    if (FEIGN_RETRYABLE_EXCEPTION.equals(className)) {
      return true;
    }
    if (!className.startsWith(FEIGN_EXCEPTION_PREFIX)) {
      return false;
    }

    Integer status = readStatus(throwable);
    return status != null && (status == 408 || status == 429 || status >= 500);
  }

  private static Integer readStatus(Throwable throwable) {
    try {
      Method statusMethod = throwable.getClass().getMethod("status");
      Object status = statusMethod.invoke(throwable);
      return status instanceof Integer value ? value : null;
    } catch (ReflectiveOperationException exception) {
      return null;
    }
  }
}
