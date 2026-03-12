package springinfra.infra.context.di.bdrpp;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import springinfra.infra.annotation.bean.Bean;
import springinfra.infra.annotation.bean.ComponentScan;
import springinfra.infra.context.di.beandef.BeanDefinition;
import springinfra.infra.context.di.beandef.BeanDefinitionRegistry;
import springinfra.infra.context.di.beandef.ConfigurationBeanDefinition;
import springinfra.infra.context.di.util.AnnotationUtils;

public class ConfigurationClassPostProcessor implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        Set<BeanDefinition> configBeanDefinitions = findConfigBeanDefinitions(registry);

        ConfigurationClassParser parser = new ConfigurationClassParser(registry);
        parser.parse(configBeanDefinitions);

        ConfigurationClassReader reader = new ConfigurationClassReader();
        reader.readAll(registry.getBeanDefinitions(ConfigurationBeanDefinition.class), registry);
    }

    @Override
    public void postProcessBeanFactory(BeanDefinitionRegistry registry) {
        FactoryBeanEnhancer enhancer = new FactoryBeanEnhancer();
        enhancer.enhance(registry);
    }

    private Set<BeanDefinition> findConfigBeanDefinitions(BeanDefinitionRegistry registry) {
        return registry.getBeanDefinitions().stream()
            .filter(bd ->
                    (hasBeanAnnotation(bd.getBeanClass()) || hasComponentScanAnnotation(bd.getBeanClass())))
                .collect(Collectors.toSet());
    }

    private boolean hasComponentScanAnnotation(Class<?> beanClass) {
        return AnnotationUtils.hasAnnotation(beanClass, ComponentScan.class);
    }

    private boolean hasBeanAnnotation(Class<?> beanClass) {
        return Arrays.stream(beanClass.getDeclaredMethods())
                .anyMatch(method -> method.isAnnotationPresent(Bean.class));
    }
}
