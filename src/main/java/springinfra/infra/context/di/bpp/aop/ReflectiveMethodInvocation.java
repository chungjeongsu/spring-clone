package springinfra.infra.context.di.bpp.aop;

import java.lang.reflect.Method;
import java.util.List;

public class ReflectiveMethodInvocation implements MethodInvocation {
    private final Object target;
    private final Method method;
    private final Object[] args;
    private final List<MethodInterceptor> interceptors;
    private int currentIndex;

    public ReflectiveMethodInvocation(
            Object target,
            Method method,
            Object[] args,
            List<MethodInterceptor> interceptors
    ) {
        this.target = target;
        this.method = method;
        this.args = args;
        this.interceptors = interceptors;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Object[] getArguments() {
        return args;
    }

    @Override
    public Object getThis() {
        return target;
    }

    @Override
    public Object proceed() throws Throwable {
        if (currentIndex == interceptors.size()) {
            return method.invoke(target, args);
        }

        MethodInterceptor next = interceptors.get(currentIndex);
        currentIndex++;
        return next.invoke(this);
    }
}
