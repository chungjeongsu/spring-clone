package springinfra.context.di.beandef;

/**
 * INFRA, COMPONENT 두 개의 빈을 공동으로 관리하는 빈 정의
 */
public class RootBeanDefinition implements BeanDefinition {
    private final BeanDefinitionType beanDefinitionType;

    private String beanName;
    private Class<?> beanClass;

    public RootBeanDefinition(BeanDefinitionType beanDefinitionType) {
        this.beanDefinitionType = beanDefinitionType;
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

    enum BeanDefinitionType {
        INFRA, COMPONENT
    }
}
