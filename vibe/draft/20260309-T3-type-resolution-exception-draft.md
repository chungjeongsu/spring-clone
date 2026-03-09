# DRAFT: T3 - 타입 주입 선택/예외 처리 보강

## 0. Meta

- 작성일: 2026-03-09
- JOB: JOB-001 프레임워크 코어 우선 완성
- TASK ID: T3
- 대상 브랜치: 현재 작업 브랜치

## 1. TASK 목표

- 문제 정의:
  - `getBean(Class)`가 후보 0건/다건을 검증하지 않고 첫 후보만 사용
  - `typeIndex.get(type)`가 `null`일 때 NPE 가능
  - 생성자 주입에서 `List<T>`, `Map<String,T>` 파라미터를 해석하지 못함
  - 예외 메시지에 후보 목록이 없어 디버깅 어려움
- 구현 목표: 타입 기반 조회를 `없음/하나/여러개`로 분기하고, `resolveParameters()`에 컬렉션 주입 라우팅을 추가
- 완료 조건(DoD):
  - 타입 미존재/중복 타입에서 예측 가능한 예외 발생
  - `getBean(Class)`, `getBeanListOfType`, `getBeanMapOfType`가 NPE 없이 동작
  - 생성자 파라미터 `List<T>`, `Map<String,T>` 주입 동작

## 2. 변경 설계 요약

- 핵심 아이디어:
  - 공통 후보 조회 메서드 `resolveCandidateNames(type)` 도입
  - `getBean(Class)`에서 후보 수 체크(0 -> not found, 2+ -> ambiguous)
  - `resolveParameters()`에서 파라미터 타입별 라우팅:
    - 일반 타입 -> `getBean(type)`
    - `List<T>` -> `getBeanListOfType(T)`
    - `Map<String,T>` -> `getBeanMapOfType(T)`
  - 메시지에 `requiredType`, `candidates` 포함
- 영향 범위:
  - 생성자 주입 실패 메시지 품질 개선
  - 컬렉션 주입으로 다중 구현체 전략 패턴 구성 가능
  - 이후 qualifier 도입 시 확장 포인트 확보

## 3. 변경 파일 목록

- `src/main/java/springinfra/infra/context/di/bean/DefaultBeanFactory.java`
- `src/main/java/springinfra/infra/context/di/exception/BeanResolveException.java` (신규)

## 4. 변경 전 코드

`DefaultBeanFactory#getBean(Class)`

```java
@Override
public <T> T getBean(Class<T> requireType) {
    Set<String> candidateNames = typeIndex.get(requireType);
    String candidateName = candidateNames.iterator().next();
    return requireType.cast(getBean(candidateName));
}
```

`DefaultBeanFactory#getBeanMapOfType`

```java
@Override
public <T> Map<String, T> getBeanMapOfType(Class<T> type) {
    Map<String, T> typeBeans = new LinkedHashMap<>();
    for(String beanName : typeIndex.get(type)) {
        typeBeans.put(beanName, type.cast(getBean(beanName)));
    }
    if(typeBeans.isEmpty()) throw new IllegalStateException("해당 타입의 빈이 없습니다! : " + type.getName());
    return typeBeans;
}
```

`DefaultBeanFactory#getBeanListOfType`

```java
@Override
public <T> List<T> getBeanListOfType(Class<T> type) {
    List<T> typeBeans = new ArrayList<>();
    for(String beanName : typeIndex.get(type)) {
        typeBeans.add(type.cast(getBean(beanName)));
    }
    if(typeBeans.isEmpty()) throw new IllegalStateException("해당 타입의 빈이 없습니다! : " + type.getName());
    return typeBeans;
}
```

`DefaultBeanFactory#resolveParameters`

```java
private Object[] resolveParameters(Constructor<?> autowirableConstructor) {
    Class<?>[] parameterTypes = autowirableConstructor.getParameterTypes();
    int parameterCount = autowirableConstructor.getParameterCount();
    Object[] parameters = new Object[parameterCount];
    for(int i = 0; i < parameterCount; i++) {
        parameters[i] = getBean(parameterTypes[i]);
    }
    return parameters;
}
```

`BeanResolveException.java` (신규 파일)

```java
```

## 5. 코드 제안

```java
@Override
public <T> T getBean(Class<T> requireType) {
    Set<String> candidateNames = resolveCandidateNames(requireType);
    if (candidateNames.isEmpty()) {
        throw new BeanResolveException("No bean found for type: " + requireType.getName());
    }
    if (candidateNames.size() > 1) {
        throw new BeanResolveException("Ambiguous bean type: " + requireType.getName()
                + ", candidates=" + candidateNames);
    }
    String candidateName = candidateNames.iterator().next();
    return requireType.cast(getBean(candidateName));
}
```

```java
private Set<String> resolveCandidateNames(Class<?> type) {
    Set<String> names = typeIndex.get(type);
    if (names == null || names.isEmpty()) {
        return Collections.emptySet();
    }
    return new LinkedHashSet<>(names);
}
```

```java
private Object[] resolveParameters(Constructor<?> autowirableConstructor) {
    Parameter[] parameters = autowirableConstructor.getParameters();
    Object[] resolved = new Object[parameters.length];

    for (int i = 0; i < parameters.length; i++) {
        resolved[i] = resolveParameter(parameters[i]);
    }
    return resolved;
}

private Object resolveParameter(Parameter parameter) {
    Class<?> rawType = parameter.getType();
    Type genericType = parameter.getParameterizedType();

    if (List.class.equals(rawType)) {
        Class<?> elementType = extractSingleGenericType(genericType, "List");
        return getBeanListOfType(elementType);
    }
    if (Map.class.equals(rawType)) {
        Class<?> valueType = extractMapValueType(genericType);
        return getBeanMapOfType(valueType);
    }
    return getBean(rawType);
}
```

## 6. 테스트 계획

- 테스트 케이스 1: 단일 후보 타입 조회 성공
- 테스트 케이스 2: 후보 없음 타입 조회 시 `BeanResolveException`
- 테스트 케이스 3: 다중 후보 타입 조회 시 `BeanResolveException` + 후보 목록 포함
- 테스트 케이스 4: 생성자 `List<T>` 주입 성공(다중 구현체 전부 주입)
- 테스트 케이스 5: 생성자 `Map<String,T>` 주입 성공(빈 이름 키 + 구현체 값)
- 테스트 케이스 6: `List/Map`의 제네릭 누락/비정상 시 예외 메시지 검증
- 테스트 케이스 7: `getBeanListOfType/getBeanMapOfType`에서 타입 미존재 시 예외 메시지 검증
- 실행 명령:
  - `./gradlew.bat test`
- 기대 결과: 타입 조회 실패가 NPE가 아닌 도메인 예외로 노출

## 7. 리뷰 체크리스트

- [ ] 타입 후보 조회가 null-safe 인가
- [ ] 모호 타입 예외 메시지에 후보명이 포함되는가
- [ ] 기존 단일 타입 주입 경로와 호환되는가
- [ ] `resolveParameters()`가 단일/리스트/맵 주입을 올바르게 라우팅하는가
- [ ] qualifier 추가 시 확장 가능한 구조인가
- [ ] 예외 클래스 네이밍/패키지가 일관적인가

## 8. 학습 포인트 (면접 대비)

- 포인트 1: DI 컨테이너의 타입 매칭 전략(정확 매칭 vs 다형성 매칭)
- 포인트 2: 실패를 NPE로 흘리지 않고 도메인 예외로 바꾸는 이유
- 포인트 3: "Ambiguous dependency"를 프레임워크가 조기에 차단해야 하는 이유

## 9. 사용자 피드백 반영 예정

- 피드백 #1: (대기)
- 반영 계획: 승인 시 코드 병합 후 테스트 결과 보고
- 상태: 대기
