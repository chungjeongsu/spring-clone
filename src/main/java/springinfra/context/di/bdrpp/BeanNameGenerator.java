package springinfra.context.di.bdrpp;

import springinfra.context.di.beandef.BeanDefinition;
import springinfra.context.di.beandef.BeanDefinitionRegistry;

public class BeanNameGenerator {
    public String generateBeanName(BeanDefinition beanDefinition, BeanDefinitionRegistry beanDefinitionRegistry) {
        Class<?> beanClass = beanDefinition.getBeanClass();
        String beanName = beanClass.getName();

        if(beanDefinitionRegistry.containsBeanDefinition(beanName)) {
            throw new IllegalArgumentException("중복된 빈 이름이 존재합니다. : " + beanName);
        }

        return toCamelCase(beanName);
    }

    private String toCamelCase(String beanName) {
        char firstChar = beanName.charAt(0);
        return Character.toLowerCase(firstChar) + beanName.substring(1);
    }
}
