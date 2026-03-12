package springinfra.infra.context.di.bpp.aop.advisor;

public class SimpleExpressionPointcut implements Pointcut {
    private final ClassMatcher classMatcher;
    private final MethodMatcher methodMatcher;

    public SimpleExpressionPointcut(String expression) {
        PointcutExpression pointcutExpression = PointcutExpression.from(expression);
        this.classMatcher = pointcutExpression::matchesClass;
        this.methodMatcher = (method, targetClass) ->
                pointcutExpression.matchesClass(targetClass)
                        && pointcutExpression.matchesMethod(method.getName());
    }

    @Override
    public ClassMatcher getClassMatcher() {
        return classMatcher;
    }

    @Override
    public MethodMatcher getMethodMatcher() {
        return methodMatcher;
    }
}
