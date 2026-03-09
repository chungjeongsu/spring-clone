# DRAFT: T5 - `@Aspect` 스캐너 구현

## 0. Meta

- 작성일: 2026-03-09
- JOB: JOB-001 프레임워크 코어 우선 완성
- TASK ID: T5
- 대상 브랜치: 현재 작업 브랜치

## 1. TASK 목표

- 문제 정의:
  - `@Aspect` 어노테이션이 없어 AOP 대상 클래스 표식이 불가능함
  - `AnnotationAdvisorFactory`가 없어 advice 메서드(`@Before`, `@Around` 등)를 Advisor로 변환할 경로가 없음
- 구현 목표:
  - `@Aspect` 클래스 스캔 후 advice 메서드 메타데이터를 읽어 Advisor 목록을 생성
  - 생성된 Advisor 후보는 T6(PointcutExpression), T7(Bean 등록)에서 후속 처리 가능하도록 반환
- 완료 조건(DoD):
  - `@Aspect` 클래스에서 advice 메서드가 Advisor로 변환됨
  - 포인트컷 문자열 파싱 책임은 T6으로 분리됨
- 완료 이후: 병합된 파일은 삭제한다.

## 2. 변경 설계 요약

- 핵심 아이디어:
  - `@Aspect`를 `@Component` 메타 어노테이션으로 선언해 기본 스캐너 경로에 포함
  - `AnnotationAdvisorFactory#create(Set<BeanDefinition>)`에서 `@Aspect` 빈 정의를 순회
  - advice 메서드 1개당 `DefaultPointcutAdvisor` 1개 생성하되 pointcut 구현체는 T6에서 주입
- 대안 비교:
  - 대안 A: 스캐너가 즉시 Bean 등록까지 수행
  - 대안 B: 스캐너는 Advisor 목록만 생성하고 등록은 T6에서 처리 (선택)
- 영향 범위:
  - T6(PointcutExpression), T7(Advisor Bean 등록)과 직접 연결됨
  - T8의 AutoProxyCreator가 조회할 Advisor 소스 품질 결정

## 3. 변경 파일 목록

- `src/main/java/springinfra/infra/annotation/aop/Aspect.java` (신규)
- `src/main/java/springinfra/infra/context/di/bpp/aop/advisor/AnnotationAdvisorFactory.java` (신규)
- `src/main/java/springinfra/infra/context/di/bpp/aop/ReflectiveAdviceMethodInterceptor.java` (신규)

## 4. 변경 전 코드

`Aspect.java` (신규 파일)

```java
```

`AnnotationAdvisorFactory.java` (신규 파일)

```java
```

`ReflectiveAdviceMethodInterceptor.java` (신규 파일)

```java
```

## 5. 코드 제안

`Aspect.java`

```java
package springinfra.infra.annotation.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import springinfra.infra.annotation.bean.Component;

@Component
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Aspect {
}
```

`AnnotationAdvisorFactory.java`

```java
public class AnnotationAdvisorFactory {
    public List<Advisor> create(Set<BeanDefinition> beanDefinitions) {
        List<Advisor> advisors = new ArrayList<>();

        for (BeanDefinition beanDefinition : beanDefinitions) {
            Class<?> beanClass = beanDefinition.getBeanClass();
            if (!beanClass.isAnnotationPresent(Aspect.class)) {
                continue;
            }

            Method[] methods = beanClass.getDeclaredMethods();
            for (Method method : methods) {
                Advisor advisor = createAdvisor(beanClass, method);
                if (advisor != null) {
                    advisors.add(advisor);
                }
            }
        }

        return advisors;
    }
}
```

`createAdvisor(...)` 규칙

```java
private Advisor createAdvisor(Class<?> aspectClass, Method adviceMethod) {
    if (adviceMethod.isAnnotationPresent(Before.class)) {
        String expression = adviceMethod.getAnnotation(Before.class).value();
        Pointcut pointcut = resolvePointcut(expression); // T6에서 PointcutExpression 기반 구현
        MethodInterceptor interceptor = new ReflectiveAdviceMethodInterceptor(aspectClass, adviceMethod, AdviceType.BEFORE);
        return new DefaultPointcutAdvisor(pointcut, interceptor);
    }

    if (adviceMethod.isAnnotationPresent(Around.class)) {
        String expression = adviceMethod.getAnnotation(Around.class).value();
        Pointcut pointcut = resolvePointcut(expression); // T6에서 PointcutExpression 기반 구현
        MethodInterceptor interceptor = new ReflectiveAdviceMethodInterceptor(aspectClass, adviceMethod, AdviceType.AROUND);
        return new DefaultPointcutAdvisor(pointcut, interceptor);
    }

    // After / AfterReturning / AfterThrowing도 동일 패턴으로 생성
    return null;
}
```

`ReflectiveAdviceMethodInterceptor.java`

```java
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
            Object result = invocation.proceed();
            invokeAdvice(invocation, result, null);
            return result;
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
        try {
            if (adviceMethod.getParameterCount() == 0) {
                return adviceMethod.invoke(aspectInstance);
            }
            if (adviceMethod.getParameterCount() == 1) {
                Class<?> parameterType = adviceMethod.getParameterTypes()[0];
                if (MethodInvocation.class.isAssignableFrom(parameterType)) {
                    return adviceMethod.invoke(aspectInstance, invocation);
                }
                if (throwable != null && Throwable.class.isAssignableFrom(parameterType)) {
                    return adviceMethod.invoke(aspectInstance, throwable);
                }
                return adviceMethod.invoke(aspectInstance, returnValue);
            }
            return adviceMethod.invoke(aspectInstance, invocation, returnValue, throwable);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}
```

## 6. 테스트 계획

- 테스트 케이스 1: `@Aspect` 없는 클래스는 Advisor 생성 대상에서 제외
- 테스트 케이스 2: `@Aspect` + `@Before` 메서드가 `DefaultPointcutAdvisor`로 변환
- 테스트 케이스 3: `@Aspect` + `@Around` 메서드가 `DefaultPointcutAdvisor`로 변환
- 테스트 케이스 4: advice 메서드 2개면 Advisor 2개 생성
- 테스트 케이스 5: 포인트컷 표현식 파싱 관련 TODO가 T6으로 분리되었는지 검증
- 실행 명령:
  - `./gradlew.bat test`
- 기대 결과: 스캐너/변환 로직 테스트 green
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

- 포인트 1: `@Aspect`를 `@Component` 메타 어노테이션으로 두는 이유
- 포인트 2: 스캔(T5)과 표현식 파싱(T6), 등록(T7)을 분리하는 설계 이점
- 포인트 3: 책임 분리로 디버깅 범위를 줄이는 방법

## 9. 사용자 피드백 반영 예정

- 피드백 #1: "스캐너는 아직 없다. 먼저 구현하자"
- 반영 계획: T5를 `@Aspect` 스캔/Advisor 변환 전용으로 분리
- 상태: 반영 완료
- 피드백 #2: "포인트컷 표현식은 Task6으로 분리"
- 반영 계획: T5에서 파서/매칭 구현 제거, T6 전용으로 이관
- 상태: 반영 완료
- 피드백 #3: (대기)
- 반영 계획: 승인 시 코드 병합 + 테스트
- 상태: 대기
