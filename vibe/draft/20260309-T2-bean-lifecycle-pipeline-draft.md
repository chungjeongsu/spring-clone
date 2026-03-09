# DRAFT: T2 - Bean 생성 파이프라인 완성

## 0. Meta

- 작성일: 2026-03-09
- JOB: JOB-001 프레임워크 코어 우선 완성
- TASK ID: T2
- 대상 브랜치: 현재 작업 브랜치

## 1. TASK 목표

- 문제 정의:
  - `DefaultBeanFactory#createBean`에서 생명주기 훅(BPP before/after)이 실제로 호출되지 않음
  - 빈 생성 예외 시 `singletonsCurrentlyCreation` 정리가 누락될 수 있음
  - BPP 적용 대상 필터가 없어 인프라/BPP 자체까지 후처리될 위험이 있음
- 구현 목표: 생성 파이프라인을 `create -> initialize(before/after) -> singleton 등록`으로 정리
- 완료 조건(DoD):
  - BPP가 모든 일반 빈에 대해 동작
  - 인프라/BPP 빈은 필터로 제외
  - 예외 발생 시 생성 중 상태가 정상 정리됨

## 2. 변경 설계 요약

- 핵심 아이디어:
  - `createBean`에서 `initializeBean` 호출
  - `initializeBean` 내부에서 `applyBeanPostProcessorsBeforeInitialization`, `applyBeanPostProcessorsAfterInitialization` 수행
  - `shouldSkipPostProcessing(beanName, bean)` 필터로 BPP 대상 제어
  - `getBeanPipeLine`에 `try/finally` 적용해 생성 상태 정리 보장
- 영향 범위:
  - BPP 기반 확장(AOP, 로깅) 적용 타이밍이 표준화됨
  - 인프라 빈 프록시로 인한 초기화 꼬임 리스크 감소

## 3. 변경 파일 목록

- `src/main/java/springinfra/infra/context/di/bean/DefaultBeanFactory.java`

## 4. 코드 제안

```java
private Object getBeanPipeLine(String beanName, BeanDefinition beanDefinition) {
    beforeSingletonCreation(beanName);
    try {
        Object beanInstance = createBean(beanName, beanDefinition);
        singletonBeans.put(beanName, beanInstance);
        return beanInstance;
    } finally {
        afterSingletonCreation(beanName);
    }
}

private Object createBean(String beanName, BeanDefinition beanDefinition) {
    Object beanInstance = createBeanInstance(beanDefinition);
    return initializeBean(beanName, beanInstance);
}

private Object initializeBean(String beanName, Object beanInstance) {
    if (shouldSkipPostProcessing(beanName, beanInstance)) {
        return beanInstance;
    }
    Object wrappedBean = applyBeanPostProcessorsBeforeInitialization(beanInstance, beanName);
    wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
    return wrappedBean;
}
```

```java
private boolean shouldSkipPostProcessing(String beanName, Object bean) {
    if (bean instanceof BeanPostProcessor) {
        return true;
    }
    BeanDefinition beanDefinition = beanDefinitions.get(beanName);
    if (beanDefinition instanceof RootBeanDefinition root
            && root.getBeanDefinitionType() == RootBeanDefinition.BeanDefinitionType.INFRA) {
        return true;
    }
    return false;
}

private Object applyBeanPostProcessorsBeforeInitialization(Object bean, String beanName) {
    Object result = bean;
    for (BeanPostProcessor bpp : beanPostProcessors) {
        result = bpp.postProcessBeforeInitialization(result, beanName);
        if (result == null) {
            throw new IllegalStateException("BeanPostProcessor returned null in beforeInitialization: " + beanName);
        }
    }
    return result;
}

private Object applyBeanPostProcessorsAfterInitialization(Object bean, String beanName) {
    Object result = bean;
    for (BeanPostProcessor bpp : beanPostProcessors) {
        result = bpp.postProcessAfterInitialization(result, beanName);
        if (result == null) {
            throw new IllegalStateException("BeanPostProcessor returned null in afterInitialization: " + beanName);
        }
    }
    return result;
}
```

## 5. 테스트 계획

- 테스트 케이스 1: BPP(before/after) 호출 순서 검증
- 테스트 케이스 2: BPP after에서 다른 객체(프록시) 반환 시 singleton에 최종 객체 등록 검증
- 테스트 케이스 3: `BeanPostProcessor` 타입 빈이 후처리 대상에서 제외되는지 검증
- 테스트 케이스 4: INFRA 타입 빈이 후처리 대상에서 제외되는지 검증
- 테스트 케이스 5: 생성 중 예외 발생 후 동일 빈 재조회 시 순환참조 오탐이 없는지 검증
- 실행 명령:
  - `./gradlew.bat test`
- 기대 결과: 컴파일/테스트 green

## 6. 리뷰 체크리스트

- [ ] BPP 훅이 정확한 순서로 실행되는가
- [ ] 후처리 스킵 필터가 의도대로 작동하는가
- [ ] 생성 실패 시 상태 정리가 보장되는가
- [ ] AOP(T4) 구현을 위한 확장 지점이 충분한가
- [ ] 기존 Bean 생성 흐름과 충돌이 없는가
- [ ] 예외 메시지가 디버깅 가능 수준인가

## 7. 학습 포인트 (면접 대비)

- 포인트 1: Bean 생명주기에서 before/after 훅의 역할 분리
- 포인트 2: try/finally로 컨테이너 상태 일관성 지키는 이유
- 포인트 3: "최종 singleton은 원본이 아닌 post-processed 객체" 원칙

## 8. 사용자 피드백 반영 예정

- 피드백 #1: (대기)
- 반영 계획: 승인 시 `DefaultBeanFactory` 코드 병합 후 테스트
- 상태: 대기
