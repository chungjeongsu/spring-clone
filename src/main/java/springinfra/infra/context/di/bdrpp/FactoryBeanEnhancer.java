package springinfra.infra.context.di.bdrpp;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.NoOp;
import springinfra.infra.annotation.bean.Bean;
import springinfra.infra.context.di.beandef.BeanDefinitionRegistry;
import springinfra.infra.context.di.beandef.ConfigurationBeanDefinition;

public class FactoryBeanEnhancer {
    public void enhance(BeanDefinitionRegistry registry) {
        Set<ConfigurationBeanDefinition> cbds = getConfigurationBeanDefinitions(registry);
        cbds.forEach(this::enhanceByCglibEnhancer);
    }

    private void enhanceByCglibEnhancer(ConfigurationBeanDefinition cbd) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(cbd.getBeanClass());
        enhancer.setUseFactory(true);

        enhancer.setCallbackTypes(new Class[]{MethodInterceptor.class, NoOp.class});

        enhancer.setCallbackFilter(method -> {
            if(method.isAnnotationPresent(Bean.class)) return 0;
            return 1;
        });

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
