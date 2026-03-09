package springinfra.infra.context.di.bpp.aop.advisor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import springinfra.infra.annotation.aop.After;
import springinfra.infra.annotation.aop.AfterReturning;
import springinfra.infra.annotation.aop.AfterThrowing;
import springinfra.infra.annotation.aop.Around;
import springinfra.infra.annotation.aop.Aspect;
import springinfra.infra.annotation.aop.Before;
import springinfra.infra.context.di.beandef.BeanDefinition;
import springinfra.infra.context.di.bpp.aop.MethodInterceptor;
import springinfra.infra.context.di.bpp.aop.ReflectiveAdviceMethodInterceptor;
import springinfra.infra.context.di.bpp.aop.ReflectiveAdviceMethodInterceptor.AdviceType;

public class AnnotationAdvisorFactory {
    public List<Advisor> create(Set<BeanDefinition> beanDefinitions) {
        List<Advisor> advisors = new ArrayList<>();

        for (BeanDefinition beanDefinition : beanDefinitions) {
            Class<?> beanClass = beanDefinition.getBeanClass();
            if (!beanClass.isAnnotationPresent(Aspect.class)) {
                continue;
            }

            Object aspectInstance = createAspectInstance(beanClass);
            Method[] methods = beanClass.getDeclaredMethods();
            for (Method method : methods) {
                Advisor advisor = createAdvisor(method, aspectInstance);
                if (advisor != null) {
                    advisors.add(advisor);
                }
            }
        }

        return advisors;
    }

    private Object createAspectInstance(Class<?> beanClass) {
        try {
            return beanClass.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("aspect instance creation failed: " + beanClass.getName(), e);
        }
    }

    private Advisor createAdvisor(Method adviceMethod, Object aspectInstance) {
        if (adviceMethod.isAnnotationPresent(Before.class)) {
            String expression = adviceMethod.getAnnotation(Before.class).value();
            return createPointcutAdvisor(expression, adviceMethod, aspectInstance, AdviceType.BEFORE);
        }

        if (adviceMethod.isAnnotationPresent(Around.class)) {
            String expression = adviceMethod.getAnnotation(Around.class).value();
            return createPointcutAdvisor(expression, adviceMethod, aspectInstance, AdviceType.AROUND);
        }

        if (adviceMethod.isAnnotationPresent(After.class)) {
            String expression = adviceMethod.getAnnotation(After.class).value();
            return createPointcutAdvisor(expression, adviceMethod, aspectInstance, AdviceType.AFTER);
        }

        if (adviceMethod.isAnnotationPresent(AfterReturning.class)) {
            String expression = adviceMethod.getAnnotation(AfterReturning.class).value();
            return createPointcutAdvisor(expression, adviceMethod, aspectInstance, AdviceType.AFTER_RETURNING);
        }

        if (adviceMethod.isAnnotationPresent(AfterThrowing.class)) {
            String expression = adviceMethod.getAnnotation(AfterThrowing.class).value();
            return createPointcutAdvisor(expression, adviceMethod, aspectInstance, AdviceType.AFTER_THROWING);
        }

        return null;
    }

    private Advisor createPointcutAdvisor(
            String expression,
            Method adviceMethod,
            Object aspectInstance,
            AdviceType adviceType
    ) {
        Pointcut pointcut = resolvePointcut(expression);
        MethodInterceptor interceptor =
                new ReflectiveAdviceMethodInterceptor(aspectInstance, adviceMethod, adviceType);
        return new DefaultPointcutAdvisor(pointcut, interceptor);
    }

    private Pointcut resolvePointcut(String expression) {
        return new SimpleExpressionPointcut(expression);
    }
}
