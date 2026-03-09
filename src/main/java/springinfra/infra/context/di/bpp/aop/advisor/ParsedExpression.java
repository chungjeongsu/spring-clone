package springinfra.infra.context.di.bpp.aop.advisor;

public class ParsedExpression {
    private final String classPattern;
    private final String methodPattern;

    private ParsedExpression(String classPattern, String methodPattern) {
        this.classPattern = classPattern;
        this.methodPattern = methodPattern;
    }

    public static ParsedExpression parse(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("pointcut expression is blank");
        }

        String trimmed = expression.trim();
        if (trimmed.contains("#")) {
            String[] parts = trimmed.split("#", -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException("pointcut format must be 'FQCN#method': " + expression);
            }

            String classPattern = parts[0].trim();
            String methodPattern = parts[1].trim();
            if (classPattern.isEmpty() || methodPattern.isEmpty()) {
                throw new IllegalArgumentException("class/method pattern is empty: " + expression);
            }
            return new ParsedExpression(classPattern, methodPattern);
        }

        if (trimmed.endsWith("*")) {
            return new ParsedExpression(trimmed, "*");
        }

        return new ParsedExpression("*", trimmed);
    }

    public String getClassPattern() {
        return classPattern;
    }

    public String getMethodPattern() {
        return methodPattern;
    }
}
