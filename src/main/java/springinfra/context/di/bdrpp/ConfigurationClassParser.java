package springinfra.context.di.bdrpp;

import java.util.List;
import java.util.Set;
import springinfra.context.di.beandef.BeanDefinition;
import springinfra.context.di.beandef.BeanDefinitionRegistry;

public class ConfigurationClassParser {
    private final ClassPathBeanDefinitionScanner scanner;

    public ConfigurationClassParser(BeanDefinitionRegistry registry) {
        this.scanner = new ClassPathBeanDefinitionScanner(registry);
    }

    public void parse(Set<BeanDefinition> configBeanDefinitions) {
        List<String> scanPackages = resolveScanPackages(configBeanDefinitions);
        scanner.scan(scanPackages);
    }

    private List<String> resolveScanPackages(Set<BeanDefinition> candidates) {
        return candidates.stream()
            .map(bd -> bd.getBeanClass().getPackageName())
            .toList();
    }

}
