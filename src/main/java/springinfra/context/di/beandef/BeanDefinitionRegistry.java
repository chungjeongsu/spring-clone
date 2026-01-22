package springinfra.context.di.beandef;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BeanDefinitionRegistry {
    private final Map<String, BeanDefinition> beanDefinitions;

    public BeanDefinitionRegistry() {
        this.beanDefinitions = new LinkedHashMap<>(256);
    }

    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) {
        if(beanDefinitions.containsKey(beanName)) {
            throw new IllegalStateException("동일한 이름의 빈이 이미 존재합니다. : " + beanName);
        }
        this.beanDefinitions.put(beanName, beanDefinition);
    }

    public boolean containsBeanDefinition(String beanName) {
        return beanDefinitions.containsKey(beanName);
    }

    public BeanDefinition getBeanDefinition(String beanName) {
        if(!containsBeanDefinition(beanName)) {
            throw new IllegalStateException("해당 BeanDefinition이 없습니다. : " + beanName);
        }
        return beanDefinitions.get(beanName);
    }

    public Set<BeanDefinition> getBeanDefinitions() {
        return new LinkedHashSet<>(beanDefinitions.values());
    }

    public <T extends BeanDefinition> Set<T> getBeanDefinitions(Class<T> beanDefinitionType) {
        return beanDefinitions.values().stream()
            .filter(beanDefinitionType::isInstance)
            .map(beanDefinitionType::cast)
            .collect(Collectors.toSet());
    }
}
