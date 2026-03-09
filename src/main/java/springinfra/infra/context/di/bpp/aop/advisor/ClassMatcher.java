package springinfra.infra.context.di.bpp.aop.advisor;

public interface ClassMatcher {
    boolean matches(Class<?> targetClass);
}
