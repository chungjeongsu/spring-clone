# DRAFT: T1 - 빌드 기준선 복구 (컴파일 에러 제거)

## 0. Meta

- 작성일: 2026-03-09
- JOB: JOB-001 프레임워크 코어 우선 완성
- TASK ID: T1
- 대상 브랜치: 현재 작업 브랜치

## 1. TASK 목표

- 문제 정의: `AutoProxyCreator#postProcessAfterInitialization`가 반환값 없이 끝나 컴파일 실패 발생
- 구현 목표: 컴파일 가능한 최소 동작으로 BeanPostProcessor 계약을 만족
- 완료 조건(DoD): `./gradlew.bat test` 실행 시 컴파일 단계 통과

## 2. 변경 설계 요약

- 핵심 아이디어: T4(AOP 본 구현) 전까지는 no-op BPP로 동작
- 대안 비교:
  - 대안 A: 임시 `return bean;` (선택)
  - 대안 B: 지금 AOP까지 한 번에 구현 (T1 범위 초과)
- 영향 범위: AOP 동작은 아직 미적용, 컴파일/부팅 안정성만 확보

## 3. 변경 파일 목록

- `src/main/java/springinfra/infra/context/di/bpp/aop/AutoProxyCreator.java`: 반환 누락 수정(no-op 반환)

## 4. 코드 제안

```java
@Override
public Object postProcessAfterInitialization(Object bean, String beanName) {
    // T1: 컴파일 안정화용 no-op.
    // T4에서 advisor 기반 프록시 생성 로직으로 교체 예정.
    return bean;
}
```

```diff
diff --git a/src/main/java/springinfra/infra/context/di/bpp/aop/AutoProxyCreator.java b/src/main/java/springinfra/infra/context/di/bpp/aop/AutoProxyCreator.java
@@
 public class AutoProxyCreator implements BeanPostProcessor {
 
     @Override
     public Object postProcessAfterInitialization(Object bean, String beanName) {
-
+        // T1: compile-safe no-op. real proxying will be added in T4.
+        return bean;
     }
 }
```

## 5. 테스트 계획

- 테스트 케이스 1: 전체 컴파일 통과 확인
- 테스트 케이스 2: 컨텍스트 부팅 시 BPP 체인으로 인한 런타임 예외가 없는지 확인
- 실행 명령:
  - `./gradlew.bat test`
- 기대 결과: `compileJava` 성공, 테스트 태스크 실행 가능 상태

## 6. 리뷰 체크리스트

- [ ] T1 범위 내 최소 수정인지
- [ ] T4 구현 여지를 남기는 구조인지
- [ ] 불필요한 기능 추가가 없는지
- [ ] 컴파일 실패 원인이 정확히 제거되는지
- [ ] 이후 테스트 기반 병합 흐름과 충돌 없는지

## 7. 학습 포인트 (면접 대비)

- 포인트 1: 프레임워크 개발에서 "컴파일 안정화"를 분리 TASK로 두는 이유
- 포인트 2: BPP 인터페이스 계약(입력 빈 반환)의 중요성
- 포인트 3: 기능 구현(T4) 전 안전한 no-op 전략의 장단점

## 8. 사용자 피드백 반영 예정

- 피드백 #1: (대기)
- 반영 계획: 승인 시 코드 병합 후 테스트 결과 첨부
- 상태: 대기
