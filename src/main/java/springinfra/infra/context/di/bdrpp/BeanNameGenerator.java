package springinfra.infra.context.di.bdrpp;

import springinfra.infra.context.di.beandef.BeanDefinitionRegistry;

/**
 * 해당 Bean의 타입을 받아, 빈의 이름을 생성해준다.
 *
 */
public class BeanNameGenerator {
    public String generateBeanName(Class<?> beanType, BeanDefinitionRegistry beanDefinitionRegistry) {
        String beanName = toCamelCase(beanType.getName());

        if(beanDefinitionRegistry.containsBeanDefinition(beanName)) {
            throw new IllegalArgumentException("중복된 빈 이름이 존재합니다. : " + beanName);
        }
        return beanName;
    }

    private String toCamelCase(String beanName) {
        char firstChar = beanName.charAt(0);
        return Character.toLowerCase(firstChar) + beanName.substring(1);
    }
}
