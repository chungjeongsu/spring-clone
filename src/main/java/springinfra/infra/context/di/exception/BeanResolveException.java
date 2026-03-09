package springinfra.infra.context.di.exception;

public class BeanResolveException extends RuntimeException {
    public BeanResolveException(String message) {
        super(message);
    }
}
