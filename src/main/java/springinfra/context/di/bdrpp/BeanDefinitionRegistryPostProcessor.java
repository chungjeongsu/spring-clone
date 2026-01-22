package springinfra.context.di.bdrpp;

import springinfra.context.di.beandef.BeanDefinitionRegistry;

public interface BeanDefinitionRegistryPostProcessor {
    void postProcessorBeanDefinitionRegistry(BeanDefinitionRegistry registry);
}
