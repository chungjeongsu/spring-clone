package springinfra.context.di.bdrpp;

import springinfra.context.di.bean.BeanFactory;
import springinfra.context.di.beandef.BeanDefinitionRegistry;

public interface BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor {
    void postProcessorBeanDefinitionRegistry(BeanDefinitionRegistry registry);
}
