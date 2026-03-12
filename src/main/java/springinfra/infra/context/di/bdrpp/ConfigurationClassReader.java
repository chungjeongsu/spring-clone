package springinfra.infra.context.di.bdrpp;

import springinfra.infra.annotation.bean.Bean;
import springinfra.infra.context.di.beandef.BeanDefinitionRegistry;
import springinfra.infra.context.di.beandef.ConfigurationBeanDefinition;
import springinfra.infra.context.di.beandef.MethodBeanDefinition;

import java.lang.reflect.Method;
import java.util.Set;

public class ConfigurationClassReader {
    public void readAll(Set<ConfigurationBeanDefinition> beanDefinitions, BeanDefinitionRegistry registry) {
        for(ConfigurationBeanDefinition cbd : beanDefinitions) {
            read(cbd, registry);
        }
    }

    public void read(ConfigurationBeanDefinition cbd, BeanDefinitionRegistry registry) {
        if(!cbd.hasFactoryMethods()) return;
        for(Method m : cbd.getBeanClass().getDeclaredMethods()) {
            if(m.isAnnotationPresent(Bean.class) && filterList(m)) {
                MethodBeanDefinition methodBeanDefinition = new MethodBeanDefinition(
                        m.getName(),
                        cbd.getBeanName(),
                        m.getName(),
                        m.getReturnType()
                );
                registry.registerBeanDefinition(methodBeanDefinition.getBeanName(), methodBeanDefinition);
            }
        }
    }

    private boolean filterList(Method m) {
        if(m.getReturnType() == Void.class) return false;
        m.setAccessible(true);
        return true;
    }
}
