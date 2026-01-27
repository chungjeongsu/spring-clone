package springinfra.context.di.bdrpp;

import springinfra.context.di.beandef.BeanDefinitionRegistry;

public interface BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor {
    void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry);
}
