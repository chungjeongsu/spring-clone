package springinfra.context.di.bdrpp;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import net.sf.cglib.proxy.Enhancer;
import springinfra.context.di.bean.BeanFactory;
import springinfra.context.di.beandef.BeanDefinitionRegistry;
import springinfra.context.di.beandef.ConfigurationBeanDefinition;

public class FactoryBeanEnhancer {
    public void enhance(BeanFactory beanFactory, BeanDefinitionRegistry registry) {
        Set<ConfigurationBeanDefinition> cbds = getConfigurationBeanDefinitions(registry);
        cbds.forEach(cbd -> enhanceByCglibEnhancer(cbd, beanFactory));
    }

    private void enhanceByCglibEnhancer(ConfigurationBeanDefinition cbd, BeanFactory beanFactory) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(cbd.getBeanClass());
        enhancer.setCallback(new FactoryMethodInterceptor(beanFactory));
        cbd.enhance(enhancer.createClass());
    }

    private Set<ConfigurationBeanDefinition> getConfigurationBeanDefinitions(
        BeanDefinitionRegistry registry
    ) {
        return registry.getBeanDefinitions().stream()
            .filter(ConfigurationBeanDefinition.class::isInstance)
            .map(ConfigurationBeanDefinition.class::cast)
            .filter(ConfigurationBeanDefinition::hasFactoryMethods)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
