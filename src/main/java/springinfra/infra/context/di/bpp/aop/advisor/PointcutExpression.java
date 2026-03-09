package springinfra.infra.context.di.bpp.aop.advisor;

public class PointcutExpression {
    private final String classPattern;
    private final String methodPattern;

    private PointcutExpression(String classPattern, String methodPattern) {
        this.classPattern = classPattern;
        this.methodPattern = methodPattern;
    }

    public static PointcutExpression from(String expression) {
        ParsedExpression parsed = ParsedExpression.parse(expression);
        return new PointcutExpression(parsed.getClassPattern(), parsed.getMethodPattern());
    }

    public boolean matchesClass(Class<?> targetClass) {
        if ("*".equals(classPattern)) {
            return true;
        }

        if (classPattern.endsWith(".*")) {
            String packagePrefix = classPattern.substring(0, classPattern.length() - 2);
            return targetClass.getName().startsWith(packagePrefix + ".");
        }

        if (classPattern.endsWith("*")) {
            String prefix = classPattern.substring(0, classPattern.length() - 1);
            return targetClass.getName().startsWith(prefix);
        }

        return targetClass.getName().equals(classPattern);
    }

    public boolean matchesMethod(String methodName) {
        if ("*".equals(methodPattern)) {
            return true;
        }

        if (methodPattern.endsWith("*")) {
            String prefix = methodPattern.substring(0, methodPattern.length() - 1);
            return methodName.startsWith(prefix);
        }

        return methodPattern.equals(methodName);
    }
}
