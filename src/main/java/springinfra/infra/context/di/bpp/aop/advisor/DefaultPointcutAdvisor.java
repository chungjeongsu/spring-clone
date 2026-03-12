package springinfra.infra.context.di.bpp.aop.advisor;

import springinfra.infra.context.di.bpp.aop.MethodInterceptor;

public class DefaultPointcutAdvisor implements Advisor {
    private final Pointcut pointcut;
    private final MethodInterceptor interceptor;

    public DefaultPointcutAdvisor(Pointcut pointcut, MethodInterceptor interceptor) {
        this.pointcut = pointcut;
        this.interceptor = interceptor;
    }

    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }

    @Override
    public MethodInterceptor getInterceptor() {
        return interceptor;
    }
}
