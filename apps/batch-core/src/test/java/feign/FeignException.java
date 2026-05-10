package feign;

public class FeignException extends RuntimeException {

  private final int status;

  public FeignException(int status) {
    super("Feign status: " + status);
    this.status = status;
  }

  public int status() {
    return status;
  }
}
