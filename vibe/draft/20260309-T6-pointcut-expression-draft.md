# DRAFT: T6 - PointcutExpression 객체 구현

## 0. Meta

- 작성일: 2026-03-09
- JOB: JOB-001 프레임워크 코어 우선 완성
- TASK ID: T6
- 대상 브랜치: 현재 작업 브랜치

## 1. TASK 목표

- 문제 정의:
  - T5 스캐너가 포인트컷 표현식 파싱/매칭 책임까지 가지면 범위 과대
  - 표현식 파싱 실패/와일드카드 정책을 독립적으로 테스트하기 어려움
- 구현 목표:
  - `PointcutExpression` 객체로 파싱/검증/매칭 책임을 캡슐화
  - `SimpleExpressionPointcut`은 `PointcutExpression`을 조합해 매칭만 수행
- 완료 조건(DoD):
  - `패키지*` 문법을 지원 (예: `springinfra.app.user.service.*`)
  - `경로#메서드*` 문법을 지원 (예: `springinfra.app.user.service.UserService#save*`)
  - `메서드명` 단독 문법을 지원 (예: `save`)
  - 잘못된 문자열(`null`, 공백, `#`, `A#`, `#b`)은 예외 처리
- 완료 이후: 병합된 파일은 삭제한다.

## 2. 변경 설계 요약

- 핵심 아이디어:
  - `ParsedExpression`은 입력 문자열을 토큰화하고 문법 타입(패키지/경로#메서드/메서드명)을 판별
  - `PointcutExpression`은 `matchesClass`, `matchesMethod`를 제공하는 값 객체
  - `SimpleExpressionPointcut`은 `PointcutExpression` 위임으로 `ClassMatcher`/`MethodMatcher` 구성
- 대안 비교:
  - 대안 A: `SimpleExpressionPointcut` 내부에서 문자열 직접 처리
  - 대안 B: 파싱 객체를 분리 (선택)
- 영향 범위:
  - T5 `AnnotationAdvisorFactory`가 pointcut 생성 시 안정적으로 재사용 가능
  - T8 테스트에서 파서 단위 검증 가능

## 3. 변경 파일 목록

- `src/main/java/springinfra/infra/context/di/bpp/aop/advisor/ParsedExpression.java` (신규)
- `src/main/java/springinfra/infra/context/di/bpp/aop/advisor/PointcutExpression.java` (신규)
- `src/main/java/springinfra/infra/context/di/bpp/aop/advisor/SimpleExpressionPointcut.java` (신규)

## 4. 변경 전 코드

`ParsedExpression.java` (신규 파일)

```java
```

`PointcutExpression.java` (신규 파일)

```java
```

`SimpleExpressionPointcut.java` (신규 파일)

```java
```

## 5. 코드 제안

`ParsedExpression.java`

```java
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
```

`PointcutExpression.java`

```java
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
            String className = targetClass.getName();
            if (className.equals(packagePrefix)) {
                return true;
            }
            return className.startsWith(packagePrefix + ".");
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
```

`SimpleExpressionPointcut.java`

```java
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
```

## 6. 테스트 계획

- 테스트 케이스 1: `springinfra.app.user.service.*` 패키지 매칭
- 테스트 케이스 2: `springinfra.app.user.service.UserService#save*` 경로#메서드 접두 매칭
- 테스트 케이스 3: `save` 메서드명 단독 매칭 (`*#save`와 동등 동작)
- 테스트 케이스 4: `*#*` 전체 매칭
- 테스트 케이스 5: 잘못된 표현식 입력 예외 검증
- 실행 명령:
  - `./gradlew.bat test`
- 기대 결과: 파서/매칭 단위 테스트 green
- 테스트는 `@DisplayName()`으로 한국어로 목적을 명확히 설명한다.
- 테스트 메서드명은 짧고 명확히 한다. ex) 빈_2개_실패

## 7. 리뷰 체크리스트

- [ ] 요구사항 충족 여부
- [ ] 기존 기능 회귀 여부
- [ ] 예외/경계값 처리
- [ ] 코드 가독성/유지보수성
- [ ] 3항 연산자 지양 원칙 준수 여부
- [ ] 테스트 커버리지 타당성

## 8. 학습 포인트 (면접 대비)

- 포인트 1: 값 객체로 파서 책임을 분리하는 이유
- 포인트 2: 포인트컷 표현식 최소 문법을 먼저 고정하는 전략
- 포인트 3: 스캐너(T5)와 파서(T6) 분리 시 디버깅 이점

## 9. 사용자 피드백 반영 예정

- 피드백 #1: "포인트컷 표현식은 Task6으로 분리"
- 반영 계획: PointcutExpression/ParsedExpression을 T6으로 독립
- 상태: 반영 완료
- 피드백 #2: "패키지* / 경로#메서드* / 메서드명 규칙"
- 반영 계획: T6 파서/매칭 문법을 해당 규칙으로 업데이트
- 상태: 반영 완료
- 피드백 #3: (대기)
- 반영 계획: 승인 시 코드 병합 + 테스트
- 상태: 대기
