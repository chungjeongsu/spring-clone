package springinfra.infra.context.di.bdrpp;

import springinfra.infra.context.di.beandef.BeanDefinitionRegistry;

public interface BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor {
    void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry);
}
