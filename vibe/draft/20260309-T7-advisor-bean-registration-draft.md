# DRAFT: T7 - Runtime Advisor Discovery Pipeline

## 0. Meta

- 작성일: 2026-03-09
- JOB: JOB-001 framework core stabilization
- TASK ID: T7
- 대상 브랜치: current working branch

## 1. TASK 목표

- 문제 정의:
  - T7을 `부팅 시 Advisor Bean 등록`으로 두면, 실제 Spring의 APC 지연 탐색 흐름과 다름.
  - `DIContext` 단계가 AOP 메타데이터 생성을 강제하게 되어 책임이 퍼짐.
- 구현 목표:
  - `AutoProxyCreator` 생성 시점에 Advisor 후보를 1회 수집한다.
  - 수집은 `Advisor bean + @Aspect scan 결과`를 합쳐서 처리한다.
- 완료 조건(DoD):
  - `DIContext`에 advisor 사전 등록 단계가 없다.
  - `AutoProxyCreator` 내부에 1회 계산된 advisor 후보 목록(`final`)이 존재한다.
  - `AnnotationAdvisorFactory.resolvePointcut()`이 `SimpleExpressionPointcut`을 사용한다.
- 완료 이후: 병합 요청 시 실제 코드에 반영한다.

## 2. 변경 설계 요약

- 핵심 아이디어:
  - Spring 유사 방식으로 APC가 advisor source를 보유한다.
  - `beanFactory.getBeanListOfType(Advisor.class)` + `annotationAdvisorFactory.create(beanFactory.getBeanDefinitions())` 결합.
  - 결합 결과를 생성자에서 불변 리스트로 1회 계산한다.
  - `DefaultBeanFactory` 자기참조 singleton을 타입 인덱스에 등록해 `AutoProxyCreator(DefaultBeanFactory)` 생성자 주입을 가능하게 한다.
- 대안 비교:
  - 대안 A: DIContext 부팅 단계에서 Advisor 생성/등록
  - 대안 B: APC 1회 계산 + 불변 보관 (선택)
- 영향 범위:
  - T8의 프록시 적용 로직이 advisor source를 APC 내부에서 바로 사용 가능.

## 3. 변경 파일 목록

- `src/main/java/springinfra/infra/context/di/bpp/aop/AutoProxyCreator.java`: advisor 1회 계산/보관 로직 추가
- `src/main/java/springinfra/infra/context/di/bpp/aop/advisor/AnnotationAdvisorFactory.java`: pointcut placeholder 제거
- `src/main/java/springinfra/infra/context/di/bean/DefaultBeanFactory.java`: singleton 타입 인덱싱 + 내부 BeanFactory singleton 등록

## 4. 변경 전 코드

`src/main/java/springinfra/infra/context/di/bpp/aop/AutoProxyCreator.java`
```java
package springinfra.infra.context.di.bpp.aop;

import springinfra.infra.context.di.bpp.BeanPostProcessor;

public class AutoProxyCreator implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // T1: compile-safe no-op. real proxying will be added in T4.
        return bean;
    }
}
```

`src/main/java/springinfra/infra/context/di/bpp/aop/advisor/AnnotationAdvisorFactory.java`
```java
private Pointcut resolvePointcut(String expression) {
    // T6에서 PointcutExpression 기반 파서/매칭 구현으로 대체 예정.
    return new Pointcut() {
        @Override
        public ClassMatcher getClassMatcher() {
            return targetClass -> false;
        }

        @Override
        public MethodMatcher getMethodMatcher() {
            return (method, targetClass) -> false;
        }
    };
}
```

`src/main/java/springinfra/infra/context/di/bean/DefaultBeanFactory.java`
```java
public class DefaultBeanFactory implements BeanDefinitionRegistry, BeanFactory {
    private final Map<String, BeanDefinition> beanDefinitions;
    private final Map<String, Object> singletonBeans;
    private final Set<String> singletonsCurrentlyCreation;
    private final Map<Class<?>, Set<String>> typeIndex;
    private final Map<Class<?>, Set<Advice>> adviceIndex;
    private final List<BeanPostProcessor> beanPostProcessors;

    public DefaultBeanFactory() {
        this.beanDefinitions = new LinkedHashMap<>(256);
        this.singletonBeans = new LinkedHashMap<>(256);
        this.singletonsCurrentlyCreation = new LinkedHashSet<>();
        this.typeIndex = new LinkedHashMap<>();
        this.beanPostProcessors = new ArrayList<>();
        this.adviceIndex = new LinkedHashMap<>();
    }

    @Override
    public void registerSingletonBean(String beanName, Object beanInstance) {
        if(singletonBeans.containsKey(beanName))
            throw new IllegalStateException("...");
        singletonBeans.put(beanName, beanInstance);
    }
}
```

## 5. 코드 제안

`src/main/java/springinfra/infra/context/di/bpp/aop/AutoProxyCreator.java`
```java
package springinfra.infra.context.di.bpp.aop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import springinfra.infra.context.di.bean.DefaultBeanFactory;
import springinfra.infra.context.di.bpp.BeanPostProcessor;
import springinfra.infra.context.di.bpp.aop.advisor.Advisor;
import springinfra.infra.context.di.bpp.aop.advisor.AnnotationAdvisorFactory;

public class AutoProxyCreator implements BeanPostProcessor {
    private final List<Advisor> candidateAdvisors;

    public AutoProxyCreator(DefaultBeanFactory beanFactory) {
        this.candidateAdvisors = initializeCandidateAdvisors(beanFactory);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // T8: apply advisors and create proxy
        return bean;
    }

    private List<Advisor> initializeCandidateAdvisors(DefaultBeanFactory beanFactory) {
        List<Advisor> advisors = new ArrayList<>();
        if (beanFactory.hasBean(Advisor.class)) {
            advisors.addAll(beanFactory.getBeanListOfType(Advisor.class));
        }

        AnnotationAdvisorFactory annotationAdvisorFactory = new AnnotationAdvisorFactory();
        advisors.addAll(annotationAdvisorFactory.create(beanFactory.getBeanDefinitions()));

        return Collections.unmodifiableList(advisors);
    }
}
```

`src/main/java/springinfra/infra/context/di/bpp/aop/advisor/AnnotationAdvisorFactory.java`
```java
private Pointcut resolvePointcut(String expression) {
    return new SimpleExpressionPointcut(expression);
}
```

`src/main/java/springinfra/infra/context/di/bean/DefaultBeanFactory.java`
```java
public class DefaultBeanFactory implements BeanDefinitionRegistry, BeanFactory {
    private static final String INTERNAL_BEAN_FACTORY_NAME = "__internalDefaultBeanFactory";

    private final Map<String, BeanDefinition> beanDefinitions;
    private final Map<String, Object> singletonBeans;
    private final Set<String> singletonsCurrentlyCreation;
    private final Map<Class<?>, Set<String>> typeIndex;
    private final Map<Class<?>, Set<Advice>> adviceIndex;
    private final List<BeanPostProcessor> beanPostProcessors;

    public DefaultBeanFactory() {
        this.beanDefinitions = new LinkedHashMap<>(256);
        this.singletonBeans = new LinkedHashMap<>(256);
        this.singletonsCurrentlyCreation = new LinkedHashSet<>();
        this.typeIndex = new LinkedHashMap<>();
        this.beanPostProcessors = new ArrayList<>();
        this.adviceIndex = new LinkedHashMap<>();
        registerSingletonBean(INTERNAL_BEAN_FACTORY_NAME, this);
    }

    @Override
    public void registerSingletonBean(String beanName, Object beanInstance) {
        if(singletonBeans.containsKey(beanName))
            throw new IllegalStateException("duplicate singleton bean: " + beanName);
        singletonBeans.put(beanName, beanInstance);

        Set<Class<?>> superTypes = getSuperTypes(beanInstance.getClass());
        for(Class<?> superType : superTypes) {
            typeIndex.computeIfAbsent(superType, values -> new LinkedHashSet<>())
                    .add(beanName);
        }
    }
}
```

## 6. 테스트 계획

- 테스트 케이스 1: APC 생성 시 advisor 후보가 1회 계산된다.
- 테스트 케이스 2: 후보 조회 시 `Advisor bean` + `@Aspect derived advisor`가 함께 포함된다.
- 테스트 케이스 3: pointcut이 placeholder(false) 대신 expression 기반으로 생성된다.
- 실행 명령:
  - `./gradlew.bat test`

## 7. 리뷰 체크리스트

- [ ] 요구사항 충족 여부
- [ ] 기존 기능 영향 여부
- [ ] 예외/경계값 처리
- [ ] 코드 가독성/유지보수성
- [ ] 3항 연산자 지양 규칙 준수 여부
- [ ] 변경 파일 목록의 모든 파일이 변경 전 코드에 포함되었는가
- [ ] 변경 파일 목록의 모든 파일이 코드 제안에 포함되었는가
- [ ] 테스트 커버리지 타당성

## 8. 학습 포인트 (면접 대비)

- 포인트 1: Spring AOP와 유사한 advisor source 수집 구조
- 포인트 2: APC 책임 경계와 컨테이너 부팅 책임 분리
- 포인트 3: 초기화 시 1회 계산 전략과 동시성 단순화 트레이드오프

## 9. 사용자 피드백 반영 이력

- 피드백 #1: "스프링처럼 APC 시점에 advisor를 찾는 구조로 변경"
- 반영 계획: T7 목표를 advisor bean 사전 등록에서 runtime discovery로 전환
- 상태: 반영 완료

