package springinfra.infra.context.di.bdrpp;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import springinfra.infra.annotation.bean.Bean;
import springinfra.infra.context.di.bean.BeanFactory;
import springinfra.infra.context.di.beandef.BeanDefinition;

public class FactoryMethodInterceptor implements MethodInterceptor {
    private final BeanFactory beanFactory;

    public FactoryMethodInterceptor(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy)
        throws Throwable {
        if(!method.isAnnotationPresent(Bean.class)) return proxy.invokeSuper(obj, args);

        Class<?> returnType = method.getReturnType();
        BeanDefinition beanDefinition = beanFactory.getBeanDefinition(returnType);

        if(beanFactory.hasBean(returnType)) {
            return beanFactory.getBean(returnType);
        }

        Object[] resolvedArgs = resolveArgs(method.getParameters());
        Object beanInstance = proxy.invokeSuper(obj, resolvedArgs);
        beanFactory.registerSingletonBean(beanDefinition.getBeanName(), beanInstance);
        return beanInstance;
    }

    private Object[] resolveArgs(Parameter[] parameters) {
        return Arrays.stream(parameters)
            .map(param -> beanFactory.getBean(param.getType()))
            .toArray();
    }
}
