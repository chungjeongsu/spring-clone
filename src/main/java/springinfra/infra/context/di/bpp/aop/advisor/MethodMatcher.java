package springinfra.infra.context.di.bpp.aop.advisor;

import java.lang.reflect.Method;

public interface MethodMatcher {
    boolean matches(Method method, Class<?> targetClass);
}
