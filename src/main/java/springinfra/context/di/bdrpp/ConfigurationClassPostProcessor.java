package springinfra.context.di.bdrpp;

import java.util.Set;
import springinfra.annotation.ComponentScan;
import springinfra.context.di.beandef.BeanDefinition;
import springinfra.context.di.beandef.BeanDefinitionRegistry;
import springinfra.context.di.beandef.ConfigurationBeanDefinition;

public class ConfigurationClassPostProcessor implements BeanDefinitionRegistryPostProcessor{

    @Override
    public void postProcessorBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        Set<ConfigurationBeanDefinition> candidates ;

        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry);
        for(ConfigurationBeanDefinition candidate : candidates) {
                Class<?> configClass = candidate.getBeanClass();
                if(configClass.isAnnotationPresent(ComponentScan.class)) {
                    String[] packages = resolveBasePackage(configClass);
                    scanner.scan(packages);
                }

                for(Method m : configClass.get)
        }


    }

    private void enhanceConfigurationBeanDefinitions(Set<ConfigClass> parsedConfigClasses, BeanDefinitionRegistry registry) {
        for(ConfigClass configClass : parsedConfigClasses) {
            String beanName = configClass.getBeanName();
            BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);

            if(beanDefinition instanceof ConfigurationBeanDefinition configurationBeanDefinition) {
                configurationBeanDefinition.enhance(configClass);
            }
        }
    }
}
