package springinfra.context.di.beandef;

/**
 * @Bean 메서드에서 생성되는 빈들의 빈 정의
 */
public class MethodBeanDefinition implements BeanDefinition {
    private String beanName;
    private Class<?> factoryClass;
    private String factoryMethod;
    private Class<?> returnType;

    @Override
    public String getBeanName() {
        if(beanName.isBlank())
            throw new IllegalStateException("beanName이 없습니다!");
        return beanName;
    }

    @Override
    public Class<?> getBeanClass() {
        if(factoryClass == null)
            throw new IllegalStateException("beanClass가 없습니다!");
        return factoryClass;
    }
}
