package springinfra.infra.context.di.beandef;

import springinfra.infra.annotation.bean.Bean;

import java.util.Arrays;

/**
 * @Configuration 클래스 전용 빈 정의
 * - factoryMethods는 @Bean을 생성하는 메서드를 의미한다.
 */
public class ConfigurationBeanDefinition implements BeanDefinition{
    private String beanName;
    private Class<?> beanClass;
    private boolean isEnhanced;

    public ConfigurationBeanDefinition(String beanName, Class<?> beanClass) {
        this.beanName = beanName;
        this.beanClass = beanClass;
    }

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

    //만약, factory methods가 비지 않았으면, proxy로 enhance 해야함
    public boolean hasFactoryMethods() {
        return Arrays.stream(beanClass.getDeclaredMethods())
                .anyMatch(m -> m.isAnnotationPresent(Bean.class));
    }

    public void enhance(Class<?> proxyClass) {
        this.beanClass = proxyClass;
        this.isEnhanced = true;
    }
}
