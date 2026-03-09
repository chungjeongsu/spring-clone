# DRAFT: T8 - AutoProxyCreator 적용

## 0. Meta

- 작성일: 2026-03-09
- JOB: JOB-001 framework core stabilization
- TASK ID: T8
- 대상 브랜치: current working branch

## 1. TASK 목표

- 문제 정의:
  - T7까지 advisor source는 준비됐지만, 실제 타겟 빈에 프록시를 입히는 로직이 없음.
- 구현 목표:
  - `AutoProxyCreator`에서 advisor 매칭 후 실제 프록시를 생성한다.
  - 인터페이스가 있으면 JDK Dynamic Proxy를 우선 사용한다.
  - 인터페이스가 없으면 CGLIB 프록시를 시도한다.
- 완료 조건(DoD):
  - `postProcessAfterInitialization`에서 advisor 적용 대상 빈은 프록시 객체로 반환된다.
  - 인프라 빈/BPP/Advisor/Aspect 자체는 프록시 대상에서 제외된다.
  - advisor가 비어있거나 매칭이 없으면 원본 빈을 그대로 반환한다.
- 완료 이후: 병합 요청 시 실제 코드에 반영한다.

## 2. 변경 설계 요약

- 핵심 아이디어:
  - `candidateAdvisors`를 기준으로 클래스/메서드 매칭을 수행하고 인터셉터 체인을 구성한다.
  - 인터페이스 기반 빈은 JDK 프록시, 클래스 기반 빈은 CGLIB 프록시를 선택한다.
- 대안 비교:
  - 대안 A: JDK 프록시만 지원
  - 대안 B: JDK + CGLIB 하이브리드 (선택)
- 영향 범위:
  - AOP 적용 시점이 Bean 초기화 후(BPP after-init)로 고정된다.
  - CGLIB 생성 실패(기본 생성자 부재 등) 시 현재 제안은 안전하게 원본 빈을 반환한다.

## 3. 변경 파일 목록

- `src/main/java/springinfra/infra/context/di/bpp/aop/AutoProxyCreator.java`: advisor 매칭 + 프록시 생성 + 인터셉터 체인 실행

## 4. 변경 전 코드

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
        if (candidateAdvisors.isEmpty()) {
            return bean;
        }

        // T8: apply advisors and create proxy.
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

## 5. 코드 제안

`src/main/java/springinfra/infra/context/di/bpp/aop/AutoProxyCreator.java`
```java
package springinfra.infra.context.di.bpp.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.sf.cglib.proxy.Enhancer;
import springinfra.infra.annotation.aop.Aspect;
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
        if (candidateAdvisors.isEmpty()) {
            return bean;
        }

        Class<?> targetClass = bean.getClass();
        if (shouldSkipProxy(bean, targetClass)) {
            return bean;
        }

        if (!hasApplicableAdvisor(targetClass)) {
            return bean;
        }

        if (hasInterfaces(targetClass)) {
            return createJdkProxy(bean, targetClass);
        }

        if (Modifier.isFinal(targetClass.getModifiers())) {
            return bean;
        }

        Object cglibProxy = createCglibProxy(bean, targetClass);
        if (cglibProxy != null) {
            return cglibProxy;
        }

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

    private boolean shouldSkipProxy(Object bean, Class<?> targetClass) {
        if (Proxy.isProxyClass(targetClass)) {
            return true;
        }
        if (bean instanceof net.sf.cglib.proxy.Factory) {
            return true;
        }
        if (bean instanceof BeanPostProcessor) {
            return true;
        }
        if (bean instanceof Advisor) {
            return true;
        }
        if (bean instanceof MethodInterceptor) {
            return true;
        }
        if (targetClass.isAnnotationPresent(Aspect.class)) {
            return true;
        }
        return false;
    }

    private boolean hasApplicableAdvisor(Class<?> targetClass) {
        Method[] methods = targetClass.getMethods();

        for (Advisor advisor : candidateAdvisors) {
            if (!advisor.getPointcut().getClassMatcher().matches(targetClass)) {
                continue;
            }

            for (Method method : methods) {
                if (isObjectMethod(method)) {
                    continue;
                }
                if (advisor.getPointcut().getMethodMatcher().matches(method, targetClass)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean hasInterfaces(Class<?> targetClass) {
        return !getAllInterfaces(targetClass).isEmpty();
    }

    private Set<Class<?>> getAllInterfaces(Class<?> targetClass) {
        Set<Class<?>> interfaces = new LinkedHashSet<>();
        Class<?> current = targetClass;

        while (current != null) {
            Class<?>[] directInterfaces = current.getInterfaces();
            for (Class<?> directInterface : directInterfaces) {
                collectInterfaceHierarchy(directInterface, interfaces);
            }
            current = current.getSuperclass();
        }

        return interfaces;
    }

    private void collectInterfaceHierarchy(Class<?> itf, Set<Class<?>> interfaces) {
        if (!interfaces.add(itf)) {
            return;
        }

        Class<?>[] parents = itf.getInterfaces();
        for (Class<?> parent : parents) {
            collectInterfaceHierarchy(parent, interfaces);
        }
    }

    private Object createJdkProxy(Object target, Class<?> targetClass) {
        Class<?>[] interfaces = getAllInterfaces(targetClass).toArray(new Class<?>[0]);

        InvocationHandler handler = (proxy, method, args) ->
                invokeWithInterceptors(target, targetClass, method, args);

        return Proxy.newProxyInstance(targetClass.getClassLoader(), interfaces, handler);
    }

    private Object createCglibProxy(Object target, Class<?> targetClass) {
        try {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(targetClass);
            enhancer.setUseFactory(true);
            enhancer.setCallback((net.sf.cglib.proxy.MethodInterceptor) (obj, method, args, proxy) ->
                    invokeWithInterceptors(target, targetClass, method, args));
            return enhancer.create();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Object invokeWithInterceptors(
            Object target,
            Class<?> targetClass,
            Method method,
            Object[] args
    ) throws Throwable {
        if (isObjectMethod(method)) {
            return method.invoke(target, safeArgs(args));
        }

        List<MethodInterceptor> interceptors = resolveInterceptors(targetClass, method);
        if (interceptors.isEmpty()) {
            return method.invoke(target, safeArgs(args));
        }

        ReflectiveMethodInvocation invocation =
                new ReflectiveMethodInvocation(target, method, safeArgs(args), interceptors);

        try {
            return invocation.proceed();
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    private List<MethodInterceptor> resolveInterceptors(Class<?> targetClass, Method method) {
        List<MethodInterceptor> interceptors = new ArrayList<>();

        for (Advisor advisor : candidateAdvisors) {
            if (!advisor.getPointcut().getClassMatcher().matches(targetClass)) {
                continue;
            }
            if (!advisor.getPointcut().getMethodMatcher().matches(method, targetClass)) {
                continue;
            }
            interceptors.add(advisor.getInterceptor());
        }

        return interceptors;
    }

    private boolean isObjectMethod(Method method) {
        return method.getDeclaringClass() == Object.class;
    }

    private Object[] safeArgs(Object[] args) {
        if (args == null) {
            return new Object[0];
        }
        return args;
    }
}
```

## 6. 테스트 계획

- 테스트 케이스 1: 매칭 advisor가 없는 빈은 원본 객체를 그대로 반환한다.
- 테스트 케이스 2: 인터페이스 구현 빈은 JDK 프록시가 생성된다.
- 테스트 케이스 3: 인터페이스 없는 빈은 CGLIB 프록시를 시도하고 실패 시 원본 빈으로 안전 복귀한다.
- 테스트 케이스 4: 매칭 메서드 호출 시 interceptor 체인이 실행된다.
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

- 포인트 1: BPP after-init에서 AOP 프록시를 적용하는 이유
- 포인트 2: JDK Proxy와 CGLIB 선택 기준(인터페이스 여부)
- 포인트 3: 프록시 생성 실패 시 graceful fallback 전략

## 9. 사용자 피드백 반영 이력

- 피드백 #1: "T8 진행"
- 반영 계획: AutoProxyCreator 실프록시 적용 설계/코드 제안 작성
- 상태: 반영 완료

## 10. Merge Incident Handling

- Merge issue detected: 없음 (DRAFT 단계)
- Impact scope: `AutoProxyCreator` 단일 파일
- PLAN/DRAFT update status: 완료
- User confirmation status: 대기
- Rollback executed: 해당 없음
- Re-merge condition: 사용자 확인 후 병합 + 테스트 통과