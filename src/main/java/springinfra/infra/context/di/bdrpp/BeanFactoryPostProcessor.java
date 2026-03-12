package springinfra.infra.context.di.bdrpp;

import springinfra.infra.context.di.beandef.BeanDefinitionRegistry;

public interface BeanFactoryPostProcessor {
    void postProcessBeanFactory(BeanDefinitionRegistry registry);
}
