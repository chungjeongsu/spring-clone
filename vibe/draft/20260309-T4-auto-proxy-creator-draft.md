# DRAFT: T4 - AOP 계약 정리

## 0. Meta

- 작성일: 2026-03-09
- JOB: JOB-001 프레임워크 코어 우선 완성
- TASK ID: T4
- 대상 브랜치: 현재 작업 브랜치

## 1. TASK 목표

- 문제 정의:
  - `Pointcut` 인터페이스가 비어 있어 Advisor 매칭 계약이 없음
  - `MethodInterceptor`가 `throws Throwable` 계약이 없어 체인 예외 전파가 제한됨
  - `ReflectiveMethodInvocation#proceed()`의 인덱스 조건이 잘못되어 체인 실행이 깨질 수 있음
- 구현 목표:
  - AOP 핵심 계약(`Pointcut`, `MethodInterceptor`, `MethodInvocation`)을 실행 가능한 형태로 확정
  - 체인 실행 로직을 안정화해 이후 T5~T7 구현의 기반을 만든다.
- 완료 조건(DoD):
  - `Pointcut`이 `ClassMatcher + MethodMatcher` 계약을 제공
  - `MethodInterceptor#invoke`가 `throws Throwable`을 지원
  - `ReflectiveMethodInvocation#proceed`가 interceptor 체인을 정상 순회
- 완료 이후: 병합된 파일은 삭제한다.

## 2. 변경 설계 요약

- 핵심 아이디어:
  - T4는 "계약/체인"만 다루고, `@Aspect` 스캔(T5), Advisor Bean 등록(T6), AutoProxyCreator 연동(T7)은 범위에서 제외
  - 인터페이스 계약을 먼저 고정해 이후 구현 단계의 변경 폭을 줄인다.
- 대안 비교:
  - 대안 A: T4에서 스캐너/APC까지 한 번에 구현
  - 대안 B: T4는 계약 정리만 수행하고 이후 TASK로 분리 (선택)
- 영향 범위:
  - AOP 관련 인터페이스/체인 호출부 컴파일 계약 변경
  - T5~T7의 구현 기준점 확정

## 3. 변경 파일 목록

- `src/main/java/springinfra/infra/context/di/bpp/aop/advisor/Pointcut.java`
- `src/main/java/springinfra/infra/context/di/bpp/aop/MethodInterceptor.java`
- `src/main/java/springinfra/infra/context/di/bpp/aop/ReflectiveMethodInvocation.java`

## 4. 변경 전 코드

`Pointcut.java`

```java
package springinfra.infra.context.di.bpp.aop.advisor;

public interface Pointcut {
}
```

`MethodInterceptor.java`

```java
package springinfra.infra.context.di.bpp.aop;

public interface MethodInterceptor {
    Object invoke(MethodInvocation invocation);
}
```

`ReflectiveMethodInvocation#proceed()`

```java
@Override
public Object proceed() throws Throwable {
    if(currentIndex == interceptors.size()-1){
        return method.invoke(target,args);
    }
    MethodInterceptor next = interceptors.get(currentIndex);
    currentIndex++;
    return next.invoke(this);
}
```

## 5. 코드 제안

`Pointcut.java`

```java
package springinfra.infra.context.di.bpp.aop.advisor;

public interface Pointcut {
    ClassMatcher getClassMatcher();
    MethodMatcher getMethodMatcher();
}
```

`MethodInterceptor.java`

```java
package springinfra.infra.context.di.bpp.aop;

public interface MethodInterceptor {
    Object invoke(MethodInvocation invocation) throws Throwable;
}
```

`ReflectiveMethodInvocation#proceed()`

```java
@Override
public Object proceed() throws Throwable {
    if (currentIndex == interceptors.size()) {
        return method.invoke(target, args);
    }

    MethodInterceptor next = interceptors.get(currentIndex);
    currentIndex++;
    return next.invoke(this);
}
```

## 6. 테스트 계획

- 테스트 케이스 1: interceptor 0개일 때 `proceed()`가 target 메서드를 직접 호출
- 테스트 케이스 2: interceptor 1개일 때 interceptor -> target 순서로 호출
- 테스트 케이스 3: interceptor 2개 이상일 때 순차 체인 호출
- 테스트 케이스 4: interceptor에서 던진 예외가 상위로 전파되는지 검증
- 실행 명령:
  - `./gradlew.bat test`
- 기대 결과: AOP 체인 계약 관련 테스트 green
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

- 포인트 1: AOP를 기능 구현 전에 "계약"부터 분리하는 이유
- 포인트 2: 체인 호출에서 오프바이원 오류가 런타임 버그로 번지는 방식
- 포인트 3: 인터셉터 예외 전파 계약(`throws Throwable`)의 필요성

## 9. 사용자 피드백 반영 예정

- 피드백 #1: "스캐너 미구현이므로 계획 더 세분화 필요"
- 반영 계획: T4는 계약 정리로 한정하고 T5~T7로 후속 분리
- 상태: 반영 완료
- 피드백 #2: (대기)
- 반영 계획: 승인 시 코드 병합 + 테스트
- 상태: 대기
