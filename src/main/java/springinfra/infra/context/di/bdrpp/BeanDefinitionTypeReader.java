package springinfra.infra.context.di.bdrpp;

import springinfra.infra.annotation.bean.Component;
import springinfra.infra.annotation.bean.Configuration;
import springinfra.infra.context.di.beandef.BeanDefinition;
import springinfra.infra.context.di.beandef.ConfigurationBeanDefinition;
import springinfra.infra.context.di.beandef.RootBeanDefinition;
import springinfra.infra.context.di.util.AnnotationUtils;

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
        return AnnotationUtils.hasAnnotation(beanClass, Configuration.class);
    }

    private boolean hasComponent(Class<?> beanClass) {
        return AnnotationUtils.hasAnnotation(beanClass, Component.class);
    }
}
