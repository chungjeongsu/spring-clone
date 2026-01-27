package springinfra.context.di.bdrpp;

import springinfra.annotation.Component;
import springinfra.annotation.Configuration;
import springinfra.context.di.beandef.BeanDefinition;
import springinfra.context.di.beandef.ConfigurationBeanDefinition;
import springinfra.context.di.beandef.RootBeanDefinition;

public class BeanDefinitionTypeReader {
    public BeanDefinition read(Class<?> beanClass, String beanName) {
        if(hasConfiguration(beanClass)) {
            return new ConfigurationBeanDefinition(beanName, beanClass);
        }
        if(hasComponent(beanClass)) {
            return new RootBeanDefinition(RootBeanDefinition.BeanDefinitionType.COMPONENT, beanName, beanClass);
        }
        return null;
    }

    private boolean hasConfiguration(Class<?> beanClass) {
        return beanClass.isAnnotationPresent(Configuration.class);
    }

    private boolean hasComponent(Class<?> beanClass) {
        return beanClass.isAnnotationPresent(Component.class);
    }
}
