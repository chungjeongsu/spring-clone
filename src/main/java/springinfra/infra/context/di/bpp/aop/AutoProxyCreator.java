package springinfra.infra.context.di.bpp.aop;

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
            for (Class<?> directInterface : current.getInterfaces()) {
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

        for (Class<?> parent : itf.getInterfaces()) {
            collectInterfaceHierarchy(parent, interfaces);
        }
    }

    private Object createJdkProxy(Object target, Class<?> targetClass) {
        Class<?>[] interfaces = getAllInterfaces(targetClass).toArray(new Class<?>[0]);
        return Proxy.newProxyInstance(
                targetClass.getClassLoader(),
                interfaces,
                (proxy, method, args) -> invokeWithInterceptors(target, targetClass, method, args)
        );
    }

    private Object createCglibProxy(Object target, Class<?> targetClass) {
        try {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(targetClass);
            enhancer.setUseFactory(true);
            enhancer.setCallback((net.sf.cglib.proxy.MethodInterceptor) (obj, method, args, methodProxy) ->
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
