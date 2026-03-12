package springinfra.infra.context.di.bpp.aop;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectiveAdviceMethodInterceptor implements MethodInterceptor {
    public enum AdviceType {
        BEFORE, AFTER, AROUND, AFTER_RETURNING, AFTER_THROWING
    }

    private final Object aspectInstance;
    private final Method adviceMethod;
    private final AdviceType adviceType;

    public ReflectiveAdviceMethodInterceptor(Object aspectInstance, Method adviceMethod, AdviceType adviceType) {
        this.aspectInstance = aspectInstance;
        this.adviceMethod = adviceMethod;
        this.adviceType = adviceType;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (adviceType == AdviceType.BEFORE) {
            invokeAdvice(invocation, null, null);
            return invocation.proceed();
        }

        if (adviceType == AdviceType.AROUND) {
            return invokeAdvice(invocation, null, null);
        }

        if (adviceType == AdviceType.AFTER) {
            try {
                return invocation.proceed();
            } finally {
                invokeAdvice(invocation, null, null);
            }
        }

        if (adviceType == AdviceType.AFTER_RETURNING) {
            Object returnValue = invocation.proceed();
            invokeAdvice(invocation, returnValue, null);
            return returnValue;
        }

        if (adviceType == AdviceType.AFTER_THROWING) {
            try {
                return invocation.proceed();
            } catch (Throwable throwable) {
                invokeAdvice(invocation, null, throwable);
                throw throwable;
            }
        }

        return invocation.proceed();
    }

    private Object invokeAdvice(MethodInvocation invocation, Object returnValue, Throwable throwable) throws Throwable {
        Object[] arguments = resolveAdviceArguments(invocation, returnValue, throwable);

        try {
            return adviceMethod.invoke(aspectInstance, arguments);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("advice method invoke failed: " + adviceMethod, e);
        }
    }

    private Object[] resolveAdviceArguments(MethodInvocation invocation, Object returnValue, Throwable throwable) {
        Class<?>[] parameterTypes = adviceMethod.getParameterTypes();
        Object[] arguments = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];

            if (MethodInvocation.class.isAssignableFrom(parameterType)) {
                arguments[i] = invocation;
                continue;
            }

            if (throwable != null && Throwable.class.isAssignableFrom(parameterType)) {
                arguments[i] = throwable;
                continue;
            }

            arguments[i] = returnValue;
        }

        return arguments;
    }
}
