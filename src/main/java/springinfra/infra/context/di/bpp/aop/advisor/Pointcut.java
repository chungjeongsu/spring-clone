package springinfra.infra.context.di.bpp.aop.advisor;

public interface Pointcut {
    ClassMatcher getClassMatcher();
    MethodMatcher getMethodMatcher();
}
