package springinfra.infra.context.di.bpp.aop.advisor;

import springinfra.infra.context.di.bpp.aop.MethodInterceptor;

public interface Advisor {
    Pointcut getPointcut();
    MethodInterceptor getInterceptor();
}
