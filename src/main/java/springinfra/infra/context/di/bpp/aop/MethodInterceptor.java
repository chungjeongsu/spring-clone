package springinfra.infra.context.di.bpp.aop;

public interface MethodInterceptor {
    Object invoke(MethodInvocation invocation) throws Throwable;
}
