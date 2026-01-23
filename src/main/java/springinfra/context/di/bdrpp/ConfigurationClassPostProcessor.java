package springinfra.context.di.bdrpp;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import springinfra.annotation.Bean;
import springinfra.annotation.ComponentScan;
import springinfra.context.di.beandef.BeanDefinition;
import springinfra.context.di.beandef.BeanDefinitionRegistry;

public class ConfigurationClassPostProcessor implements BeanDefinitionRegistryPostProcessor{

    @Override
    public void postProcessorBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        Set<BeanDefinition> configCandidates = findConfigBeanDefinition(registry);

        ConfigurationClassParser parser = new ConfigurationClassParser(registry);
        parser.parse(configCandidates);
    }

    private Set<BeanDefinition> findConfigBeanDefinition(BeanDefinitionRegistry registry) {
        return registry.getBeanDefinitions().stream()
            .filter(beanDefinition -> {
                Class<?> clazz = beanDefinition.getBeanClass();
                if(clazz.isAnnotationPresent(ComponentScan.class)) return true;
                return hasBeanAnnotation(clazz);
            }).collect(Collectors.toSet());
    }

    private boolean hasBeanAnnotation(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredMethods()).anyMatch(
                method -> method.isAnnotationPresent(Bean.class)
            );
    }
}
