package springinfra.context.di.beandef;

/**
 * @Bean 메서드에서 생성되는 빈들의 빈 정의
 */
public class MethodBeanDefinition implements BeanDefinition {
    private String beanName;
    private String factoryBeanName;
    private String factoryMethod;
    private Class<?> returnType;

    public MethodBeanDefinition(String beanName, String factoryBeanName, String factoryMethod, Class<?> returnType) {
        this.beanName = beanName;
        this.factoryBeanName = factoryBeanName;
        this.factoryMethod = factoryMethod;
        this.returnType = returnType;
    }

    @Override
    public String getBeanName() {
        if(beanName.isBlank())
            throw new IllegalStateException("beanName이 없습니다!");
        return beanName;
    }

    @Override
    public Class<?> getBeanClass() {
        if(returnType == null)
            throw new IllegalStateException("beanClass가 없습니다!");
        return returnType;
    }

    public String getFactoryBeanName() {
        return factoryBeanName;
    }

    public String getFactoryMethod() {
        return factoryMethod;
    }
}
