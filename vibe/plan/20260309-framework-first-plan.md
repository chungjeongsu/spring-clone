# PLAN: JOB-001 프레임워크 코어 우선 완성

## 0. Meta

- 작성일: 2026-03-09
- 프로젝트: spring-clone
- 작성자: AI Agent
- 방향: 기능 개발 전, Spring Clone 프레임워크 코어 안정화 우선

## 1. JOB 분석

- JOB 목적: 앱 기능보다 먼저 DI/AOP/라이프사이클이 동작하는 프레임워크 기반을 완성한다.
- 기대 결과:
  - 부팅/빈 생성/주입/후처리가 안정적으로 실행된다.
  - AOP 프록시 생성이 동작한다.
  - 테스트 게이트로 프레임워크 회귀를 막을 수 있다.
- 범위(In Scope):
  - 컴파일 깨진 코드 복구
  - Bean 생성 파이프라인 정리
  - AutoProxyCreator 구현
  - 프레임워크 단위/통합 테스트 작성
- 제외 범위(Out of Scope):
  - 사용자 도메인 기능 확장
  - 외부 DB/HTTP 서버 연동

## 2. 현재 상태 진단

- `AutoProxyCreator#postProcessAfterInitialization` 미구현으로 컴파일 실패
- `DefaultBeanFactory`에 BeanPostProcessor 훅은 있으나 생성 파이프라인 적용이 불완전
- 테스트 코드 디렉터리/의존성 부재로 회귀 방지 장치 없음

## 3. TASK 분해

| TASK ID | 작업명 | 목적 | 대상 파일(예상) | CS 학습 포인트 | 완료 조건(DoD) |
|---|---|---|---|---|---|
| T1 | 컴파일 기준선 복구 | 개발/테스트 가능한 최소 상태 확보 | `infra/context/di/bpp/aop/AutoProxyCreator.java` | 컴파일 단계/빌드 파이프라인 | `./gradlew.bat test` 컴파일 통과 |
| T2 | Bean 생성 파이프라인 완성 | 생성-주입-후처리 흐름 안정화 | `infra/context/di/bean/DefaultBeanFactory.java` | 객체 생명주기, IoC | BPP before/after 훅 정상 적용 |
| T3 | 타입 주입/예외 처리 보강 | 모호성/미존재 타입 처리 + 컬렉션 주입 지원 | `DefaultBeanFactory`, 예외 클래스 | 타입 시스템, 제네릭 리플렉션, 실패 설계 | 모호 타입/미존재 타입 예외 + `resolveParameters()`의 `List<T>/Map<String,T>` 주입 규칙 확정 |
| T4 | AOP 계약 정리 | Pointcut/Interceptor/Invocation 인터페이스를 실행 가능한 형태로 확정 | `bpp/aop/*`, `advisor/*` | 호출 체인, 인터페이스 설계 | 체인 실행/포인트컷 매칭 API가 컴파일 및 런타임 기준 충족 |
| T5 | `@Aspect` 스캐너 구현 | 어노테이션 기반 advice 메타데이터를 Advisor 후보로 변환 | `annotation/aop/Aspect.java`, `advisor/AnnotationAdvisorFactory.java` | 리플렉션 스캔, 메타데이터 파싱 | `@Aspect` 클래스의 advice 메서드가 Advisor 후보 목록으로 생성 |
| T6 | PointcutExpression 객체 구현 | 포인트컷 표현식 파싱/매칭 책임 분리 | `advisor/PointcutExpression.java`, `advisor/ParsedExpression.java`, `advisor/SimpleExpressionPointcut.java` | 파서 설계, 표현식 모델링 | `패키지*`, `경로#메서드*`, `메서드명` 문법 파싱/매칭 동작 및 예외 케이스 확정 |
| T7 | Advisor Bean 등록 파이프라인 | 생성된 Advisor를 부팅 시 Bean으로 등록 | `bdrpp/AdvisorBeanRegistrar.java`, `DIContext.java` | 부팅 단계 설계, 컨테이너 확장 | Advisor source(기존 Bean + 스캔 결과)가 BeanFactory에 통합 등록 |
| T8 | AutoProxyCreator 연동 | Advisor Bean을 조회해 실제 프록시 적용 | `bpp/aop/AutoProxyCreator.java`, `DIContext.java` | JDK Proxy/CGLIB 선택, AOP 적용 시점 | 대상 빈에서 advice 적용/비적용 경로가 의도대로 동작 |
| T9 | 프레임워크 테스트 세팅 | 병합 게이트 구축 | `build.gradle`, `src/test/java/...` | 단위/통합 테스트 전략 | DI/AOP 핵심 시나리오 green |

## 4. 구현 전략

- 1단계: T1로 깨진 빌드부터 즉시 복구
- 2단계: T2/T3로 DI 파이프라인 안정화
- 3단계: T4로 AOP 계약(포인트컷/인터셉터/호출체인) 확정
- 4단계: T5로 `@Aspect` 스캔 기반 Advisor 후보 생성
- 5단계: T6로 PointcutExpression 파서/매칭 객체 완성
- 6단계: T7으로 Advisor Bean 등록 파이프라인 완성
- 7단계: T8에서 AutoProxyCreator 프록시 적용 완성
  - AutoProxyCreator는 런타임 스캔 대신 `getBeanListOfType(Advisor.class)`로 조회
- 8단계: T9에서 테스트로 회귀 방지 고정

## 5. 테스트 전략

- 단위 테스트:
  - 생성자 주입 성공/실패
  - 동일 타입 다중 후보 예외
  - 생성자 파라미터 `List<T>` 주입 검증
  - 생성자 파라미터 `Map<String,T>` 주입 검증
  - BPP 적용 순서
- 통합 테스트:
  - `DIContext.refresh()` 전체 부팅
  - `AnnotationAdvisorFactory`가 `@Aspect`를 스캔해 Advisor를 생성하는지 확인
  - `PointcutExpression` 파서/매칭 결과가 의도대로 동작하는지 확인
  - 부팅 시 `@Aspect` 기반 Advisor가 Bean으로 등록되는지 확인
  - AOP 대상 메서드 호출 시 interceptor 체인 적용
- 실행 명령:
  - `./gradlew.bat test`
  - `./gradlew.bat clean test`

## 6. 리스크 및 대응

- 리스크 1: 프록시 적용 시 원본 타입 캐스팅 이슈
- 대응 1: 인터페이스 기반 우선 + CGLIB fallback 정책 명시
- 리스크 2: 파이프라인 수정으로 기존 빈 생성 순서 깨짐
- 대응 2: 통합 테스트에 부팅/빈 조회 시나리오 고정
- 리스크 3: 런타임 시 advisor 재계산으로 인한 동작 비결정성
- 대응 3: advisor는 부팅 시점 Bean 등록으로 고정, 런타임 동적 갱신은 범위 밖으로 분리
- 리스크 4: `@Aspect` 스캐너 미구현으로 T4 핵심 경로 미완성
- 대응 4: T4~T7로 분할하여 `계약 확정 -> 스캐너 구현 -> Bean 등록 -> APC 연동` 순서로 고정
- 리스크 5: 표현식 파서까지 T5에 넣으면 범위 과대
- 대응 5: T6(PointcutExpression)로 분리해 스캐너와 파서를 독립 개발

## 7. 질문 유도(학습용)

- Q1. Bean 생명주기 훅을 어디에 두어야 프록시와 초기화 충돌을 줄일 수 있을까?
- Q2. JDK Dynamic Proxy와 CGLIB의 선택 기준을 이 프로젝트에서 어떻게 둘까?
- Q3. 프레임워크 레벨 테스트가 앱 레벨 테스트보다 먼저 필요한 이유는 무엇일까?

## 8. 사용자 피드백 반영 로그

- 피드백 #1: "프레임워크 먼저"
- 반영 방식: 앱 기능 PLAN 대신 프레임워크 코어 PLAN으로 전환
- 상태: 반영 완료
- 피드백 #2: "생성자 주입은 resolveParameters 기준으로 List/Map도 지원"
- 반영 방식: T3 범위에 컬렉션 주입 규칙 추가
- 상태: 반영 완료
- 피드백 #3: "Advisor 생성 시점 통일(부팅 시) 적용"
- 반영 방식: T4 범위에 advisor Bean 부팅 등록 전략 추가
- 상태: 반영 완료
- 피드백 #4: "`@Aspect` 스캔 기반 advisor 생성 반영"
- 반영 방식: T4 범위에 `@Aspect` 스캔 -> Advisor 변환 규칙 추가
- 상태: 반영 완료
- 피드백 #5: "Pointcut/MethodInterceptor는 Advisor 생성 시 결합, 스캐너 필요"
- 반영 방식: T4 범위에 `AnnotationAdvisorFactory` 구현을 명시하고 생성 규칙을 DoD로 고정
- 상태: 반영 완료
- 피드백 #6: "스캐너 미구현이므로 계획 더 세분화 필요"
- 반영 방식: 기존 T4/T5를 T4~T8로 분할
- 상태: 반영 완료
- 피드백 #7: "포인트컷 표현식은 Task6으로 분리"
- 반영 방식: T6를 PointcutExpression 전용 TASK로 신설하고 후속 TASK 번호 조정
- 상태: 반영 완료
- 피드백 #8: "T6 표현식 규칙: 패키지* / 경로#메서드* / 메서드명"
- 반영 방식: T6 DoD와 파서 규칙을 해당 문법으로 업데이트
- 상태: 반영 완료
