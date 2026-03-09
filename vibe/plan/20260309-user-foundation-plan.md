# PLAN: JOB-001 사용자 등록/조회 + DI 검증 + 테스트 기반 세팅

## 0. Meta

- 작성일: 2026-03-09
- 프로젝트: spring-clone
- 작성자: AI Agent
- 관련 요청: 백엔드 포트폴리오/기초지식/AI 활용을 위한 VIBE 코딩 첫 사이클 시작

## 1. JOB 분석

- JOB 목적: Spring Clone 위에서 실제 백엔드 흐름(Controller -> Service -> Repository -> DI)을 구현 가능한 형태로 첫 완료한다.
- 기대 결과:
  - 사용자 등록/조회 유즈케이스가 동작한다.
  - DI 주입 흐름이 테스트로 검증된다.
  - 병합 전 테스트 게이트를 운영할 수 있다.
- 범위(In Scope):
  - 컴파일 실패 원인 제거(현재 AOP BPP 클래스)
  - User 도메인/저장소/서비스/컨트롤러 최소 기능
  - 테스트 프레임워크 도입 및 핵심 테스트 작성
- 제외 범위(Out of Scope):
  - 실제 HTTP 서버/소켓 처리
  - DB 연동(JPA, JDBC)
  - 인증/인가

## 2. 현재 상태 진단

- 현재 코드 구조:
  - 커스텀 DI 컨텍스트와 BeanDefinition/BeanFactory 인프라가 존재
  - `app.user`는 `UserController` 빈 클래스, `AppConfig`, `DefaultBean`만 존재
- 재사용 가능 컴포넌트:
  - `DIContext`, bean annotation 세트, 스캔/등록 로직
- 확인된 이슈/제약:
  - 기준선 테스트 실행 불가: `AutoProxyCreator#postProcessAfterInitialization` 반환 누락으로 컴파일 실패
  - `src/test` 디렉터리 및 테스트 의존성 부재

## 3. TASK 분해

| TASK ID | 작업명 | 목적 | 대상 파일(예상) | CS 학습 포인트 | 완료 조건(DoD) |
|---|---|---|---|---|---|
| T1 | 빌드 안정화(컴파일 에러 제거) | 테스트 가능한 기준선 확보 | `src/main/java/springinfra/infra/context/di/bpp/aop/AutoProxyCreator.java` | JVM 컴파일 단계와 CI 게이트 | `./gradlew.bat test`에서 컴파일 단계 통과 |
| T2 | User 도메인 + 저장소 구현 | 상태 저장/조회의 최소 백엔드 모델 구축 | `src/main/java/springinfra/app/user/domain/*`, `.../repository/*` | 메모리 모델, 컬렉션 동시성 기초 | 등록/조회 메서드 동작 및 예외 정책 정의 |
| T3 | Service 계층 + DI 연결 | 비즈니스 규칙을 인프라와 분리 | `src/main/java/springinfra/app/user/service/*`, `.../config/AppConfig.java` | DI/IoC, 계층 분리 원칙 | 컨트롤러에서 서비스 호출 가능한 주입 완료 |
| T4 | Controller 유즈케이스 메서드 | 포트폴리오에서 설명 가능한 엔드포인트 대응 계층 확보 | `src/main/java/springinfra/app/user/controller/UserController.java` | API 경계 설계, 입력 검증 책임 | register/find 메서드 및 실패 케이스 정의 |
| T5 | 테스트 기반 구축 + 핵심 테스트 | 병합 게이트 운영 가능 상태 달성 | `build.gradle`, `src/test/java/...` | 단위/통합 테스트 분리, 회귀 방지 | 유닛+통합 테스트 작성 및 `test` green |

## 4. 구현 전략

- 설계 선택지 A: 애노테이션 기반 컴포넌트 스캔만 사용
  - 장점: 스프링과 유사한 경험
  - 단점: 초기 디버깅 난이도 상승
- 설계 선택지 B: `@Configuration` + `@Bean` 명시 등록 중심
  - 장점: 의존관계가 파일에 명확히 드러남
  - 단점: 보일러플레이트 증가
- 최종 선택: B를 기본으로 진행 후, 안정화되면 A를 확장한다.

## 5. 테스트 전략

- 단위 테스트:
  - UserRepository: 저장/조회/없는 ID 조회
  - UserService: 중복 사용자 처리, 조회 실패 처리
- 통합 테스트:
  - DIContext 부팅 후 Controller -> Service -> Repository 연결 확인
- 회귀 테스트:
  - 기존 DI 초기화 경로가 깨지지 않는지 점검
- 실행 명령:
  - `./gradlew.bat test`
  - `./gradlew.bat clean test`

## 6. 리스크 및 대응

- 리스크 1: 인프라 리팩터링 중 Bean 생성 순서 이슈
- 대응 1: T1에서 컴파일 안정화 후 T2~T5를 작은 커밋 단위로 분리
- 리스크 2: 테스트 환경 미구축으로 병합 게이트 무력화
- 대응 2: T5를 필수 게이트로 지정(테스트 green 전 병합 금지)

## 7. 질문 유도(학습용)

- Q1. 왜 Controller에 비즈니스 로직을 넣지 않고 Service로 분리해야 할까?
- Q2. 커스텀 DI 컨테이너에서 순환 참조가 발생하면 어떤 방식으로 탐지/완화할 수 있을까?
- Q3. In-memory Repository 설계에서 동시성 이슈는 언제부터 실제 문제가 될까?

## 8. 사용자 피드백 반영 로그

- 피드백 #1: (대기)
- 반영 방식: PLAN 확정 후 TASK별 DRAFT 생성 시 반영
- 상태: 대기
