# DRAFT: T9 - 프레임워크 테스트 하드닝

## 0. Meta

- 작성일: 2026-03-10
- JOB: JOB-001 framework core stabilization
- TASK ID: T9
- 대상 브랜치: current working branch

## 1. TASK 목표

- 문제 정의:
  - 현재 테스트 소스가 없어 DI/AOP 회귀를 막을 안전장치가 없다.
- 구현 목표:
  - JDK Proxy, CGLIB Proxy, non-match 시 원본 반환 경로를 테스트로 고정한다.
  - interceptor 체인 실행 순서를 테스트로 고정한다.
- 완료 조건(DoD):
  - `./gradlew.bat test`에서 T9 테스트가 실행/성공한다.
  - AOP 핵심 경로(매칭, 프록시 선택, 체인 실행, 미매칭)가 검증된다.
- 완료 이후: 병합 요청 시 실제 코드에 반영한다.

## 2. 변경 설계 요약

- 핵심 아이디어:
  - `AutoProxyCreator` 단위 테스트로 AOP 핵심 동작을 고정한다.
  - advisor는 `DefaultPointcutAdvisor + SimpleExpressionPointcut`으로 구성한다.
  - `DefaultBeanFactory.registerSingletonBean(...)` 경로를 통해 advisor 등록 후 APC 생성한다.
- 대안 비교:
  - 대안 A: DIContext 통합 테스트 중심
  - 대안 B: APC 단위 테스트 중심 + 최소 통합 신호 (선택)
- 영향 범위:
  - 테스트 의존성(JUnit5) 추가
  - 운영 코드 변경 없음

## 3. 변경 파일 목록

- `build.gradle`: JUnit5 테스트 의존성/테스트 런처 설정 추가
- `src/test/java/springinfra/infra/context/di/bpp/aop/AutoProxyCreatorTest.java`: T9 핵심 테스트 추가

## 4. 변경 전 코드

`build.gradle`
```gradle
plugins {
    id 'java'
    id 'application'
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'cglib:cglib:3.3.0'
}

application {
    mainClass = 'springinfra.SpringClonePjApplication'
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}

tasks.withType(JavaExec).configureEach {
    jvmArgs "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dsun.jnu.encoding=UTF-8",
            "--add-opens=java.base/java.lang=ALL-UNNAMED"
}
```

`src/test/java/springinfra/infra/context/di/bpp/aop/AutoProxyCreatorTest.java`
```java
```

## 5. 코드 제안

`build.gradle`
```gradle
plugins {
    id 'java'
    id 'application'
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'cglib:cglib:3.3.0'

    testImplementation 'org.junit.jupiter:junit-jupiter-api:5.10.2'
    testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.10.2'
}

application {
    mainClass = 'springinfra.SpringClonePjApplication'
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}

tasks.withType(JavaExec).configureEach {
    jvmArgs "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dsun.jnu.encoding=UTF-8",
            "--add-opens=java.base/java.lang=ALL-UNNAMED"
}

tasks.withType(Test).configureEach {
    useJUnitPlatform()
}
```

`src/test/java/springinfra/infra/context/di/bpp/aop/AutoProxyCreatorTest.java`
```java
package springinfra.infra.context.di.bpp.aop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import springinfra.infra.context.di.bean.DefaultBeanFactory;
import springinfra.infra.context.di.bpp.aop.advisor.Advisor;
import springinfra.infra.context.di.bpp.aop.advisor.DefaultPointcutAdvisor;
import springinfra.infra.context.di.bpp.aop.advisor.SimpleExpressionPointcut;

class AutoProxyCreatorTest {

    @Test
    @DisplayName("advisor 미등록이면 원본 빈을 그대로 반환")
    void returnOriginalWhenNoAdvisor() {
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        AutoProxyCreator autoProxyCreator = new AutoProxyCreator(beanFactory);

        TestService target = new TestServiceImpl(new ArrayList<>());
        Object result = autoProxyCreator.postProcessAfterInitialization(target, "testService");

        assertSame(target, result);
    }

    @Test
    @DisplayName("인터페이스 구현 빈은 JDK 프록시가 생성되고 interceptor 체인이 실행")
    void createJdkProxyAndInvokeInterceptorChain() {
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        List<String> trace = new ArrayList<>();
        registerAdvisor(beanFactory, "*#save", trace);

        AutoProxyCreator autoProxyCreator = new AutoProxyCreator(beanFactory);

        TestService target = new TestServiceImpl(trace);
        Object proxied = autoProxyCreator.postProcessAfterInitialization(target, "testService");

        assertTrue(Proxy.isProxyClass(proxied.getClass()));

        TestService service = (TestService) proxied;
        service.save("kim");

        assertEquals(List.of("before", "target:kim", "after"), trace);
    }

    @Test
    @DisplayName("인터페이스 없는 빈은 CGLIB 프록시를 시도하고 interceptor 체인이 실행")
    void createCglibProxyForConcreteClass() {
        DefaultBeanFactory beanFactory = new DefaultBeanFactory();
        List<String> trace = new ArrayList<>();
        registerAdvisor(beanFactory, "*#save", trace);

        AutoProxyCreator autoProxyCreator = new AutoProxyCreator(beanFactory);

        ConcreteService target = new ConcreteService(trace);
        Object proxied = autoProxyCreator.postProcessAfterInitialization(target, "concreteService");

        assertFalse(Proxy.isProxyClass(proxied.getClass()));
        assertNotSame(target, proxied);

        ConcreteService service = (ConcreteService) proxied;
        service.save("lee");

        assertEquals(List.of("before", "target:lee", "after"), trace);
    }

    private void registerAdvisor(DefaultBeanFactory beanFactory, String expression, List<String> trace) {
        Advisor advisor = new DefaultPointcutAdvisor(
                new SimpleExpressionPointcut(expression),
                invocation -> {
                    trace.add("before");
                    Object result = invocation.proceed();
                    trace.add("after");
                    return result;
                }
        );

        beanFactory.registerSingletonBean("testAdvisor", advisor);
    }

    interface TestService {
        void save(String name);
    }

    static class TestServiceImpl implements TestService {
        private final List<String> trace;

        TestServiceImpl(List<String> trace) {
            this.trace = trace;
        }

        @Override
        public void save(String name) {
            trace.add("target:" + name);
        }
    }

    static class ConcreteService {
        private final List<String> trace;

        ConcreteService(List<String> trace) {
            this.trace = trace;
        }

        public void save(String name) {
            trace.add("target:" + name);
        }
    }
}
```

## 6. 테스트 계획

- 테스트 케이스 1: advisor 미등록 시 원본 반환
- 테스트 케이스 2: 인터페이스 빈 JDK 프록시 + 체인 실행
- 테스트 케이스 3: 구체 클래스 빈 CGLIB 프록시 + 체인 실행
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

- 포인트 1: BPP 단계에서 프록시 생성이 테스트에 주는 이점
- 포인트 2: JDK Proxy/CGLIB 선택 규칙을 테스트로 고정하는 방법
- 포인트 3: interceptor 체인 순서 검증 포인트

## 9. 사용자 피드백 반영 이력

- 피드백 #1: "t9 ㄱㄱ"
- 반영 계획: 테스트 하드닝 T9 DRAFT 작성
- 상태: 반영 완료

## 10. Merge Incident Handling

- Merge issue detected: 없음 (DRAFT 단계)
- Impact scope: 테스트/빌드 설정
- PLAN/DRAFT update status: 완료
- User confirmation status: 대기
- Rollback executed: 해당 없음
- Re-merge condition: 사용자 확인 후 병합 + 테스트 통과