package feign;

public class RetryableException extends RuntimeException {

  public RetryableException() {
    super("retryable");
  }
}
