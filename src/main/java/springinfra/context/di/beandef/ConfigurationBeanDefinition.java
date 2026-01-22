package springinfra.context.di.beandef;

import java.util.Set;

/**
 * @Configuration 클래스 전용 빈 정의
 * - factoryMethods는 @Bean을 생성하는 메서드를 의미한다.
 */
public class ConfigurationBeanDefinition implements BeanDefinition{
    private String beanName;
    private Class<?> beanClass;
    private Set<String> factoryMethods;

    @Override
    public String getBeanName() {
        if(beanName.isBlank())
            throw new IllegalStateException("beanName이 없습니다!");
        return beanName;
    }

    @Override
    public Class<?> getBeanClass() {
        if(beanClass == null)
            throw new IllegalStateException("beanClass가 없습니다!");
        return beanClass;
    }
}
