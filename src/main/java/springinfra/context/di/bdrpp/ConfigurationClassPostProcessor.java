package springinfra.context.di.bdrpp;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import springinfra.annotation.Bean;
import springinfra.annotation.ComponentScan;
import springinfra.context.di.bean.BeanFactory;
import springinfra.context.di.beandef.BeanDefinition;
import springinfra.context.di.beandef.BeanDefinitionRegistry;
import springinfra.context.di.beandef.ConfigurationBeanDefinition;

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
    public void postProcessBeanFactory(BeanFactory beanFactory, BeanDefinitionRegistry registry) {
        FactoryBeanEnhancer enhancer = new FactoryBeanEnhancer();
        enhancer.enhance(beanFactory, registry);
    }

    private Set<BeanDefinition> findConfigBeanDefinitions(BeanDefinitionRegistry registry) {
        return registry.getBeanDefinitions().stream()
            .filter(bd ->
                    (hasBeanAnnotation(bd.getBeanClass()) || hasComponentScanAnnotation(bd.getBeanClass())))
                .collect(Collectors.toSet());
    }

    private boolean hasComponentScanAnnotation(Class<?> beanClass) {
        return beanClass.isAnnotationPresent(ComponentScan.class);
    }

    private boolean hasBeanAnnotation(Class<?> beanClass) {
        return Arrays.stream(beanClass.getDeclaredMethods())
                .anyMatch(method -> method.isAnnotationPresent(Bean.class));
    }
}
