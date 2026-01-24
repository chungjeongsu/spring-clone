package springinfra.context.di.beandef;

import java.util.Set;

public interface BeanDefinitionRegistry {
    void registerBeanDefinition(String beanName, BeanDefinition beanDefinition);

    boolean containsBeanDefinition(String beanName);

    BeanDefinition getBeanDefinition(String beanName);

    Set<BeanDefinition> getBeanDefinitions();

    <T extends BeanDefinition> Set<T> getBeanDefinitions(Class<T> beanDefinitionType);
}
