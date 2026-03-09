package springinfra.infra.context.di.exception;

public class BeanScanException extends RuntimeException {
  public BeanScanException(String message) {
    super(message);
  }

  public BeanScanException(String message, Throwable e) {
    super(message, e);
  }
}
