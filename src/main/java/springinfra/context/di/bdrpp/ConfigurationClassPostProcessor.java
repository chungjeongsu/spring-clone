package springinfra.context.di.bdrpp;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import springinfra.annotation.Bean;
import springinfra.annotation.ComponentScan;
import springinfra.context.di.bean.BeanFactory;
import springinfra.context.di.beandef.BeanDefinition;
import springinfra.context.di.beandef.BeanDefinitionRegistry;
import springinfra.context.di.beandef.ConfigurationBeanDefinition;

public class ConfigurationClassPostProcessor implements BeanDefinitionRegistryPostProcessor {
    /**
     * BeanDefinitionRegistryPostProcessor의 역할(BDRPP)
     * - @ComponentScan 이나 @Bean이 붙은 BeanDefinition
     *      - 해당 어노테이션은 또 다른 BeanDefinition을 등록해야하는 선언임
     *      - 때문에, 해당 어노테이션 파싱 시 @Component와 같은 일반 Bean Definition을 모두 찾을 수 있음
     *
     *  1. 기본 @Configuration, @Component를 모두 BeanDefinition으로 등록
     *  2. 그 후 @Configuration BeanDefinition을 보고 @Bean 의 BeanDefinition을 생성
     */
    @Override
    public void postProcessorBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        Set<BeanDefinition> configBeanDefinitions = findConfigBeanDefinition(registry);

        ConfigurationClassParser parser = new ConfigurationClassParser(registry);
        parser.parse(configBeanDefinitions);

        ConfigurationClassReader reader = new ConfigurationClassReader();
        reader.readAll(registry.getBeanDefinitions(ConfigurationBeanDefinition.class), registry);
    }

    /**
     * BeanFactoryPostProcessor의 역할(BFPP)
     * - @Bean이 있는 ConfigurationBeanDefinition들을 찾아, 프록시 적용
     * - @Bean 메서드는 객체를 반환하는 것이 아니라, Bean을 반환하는 factory method이기 때문
     */
    @Override
    public void postProcessBeanFactory(BeanFactory beanFactory, BeanDefinitionRegistry registry) {
        FactoryBeanEnhancer enhancer = new FactoryBeanEnhancer();
        enhancer.enhance(beanFactory, registry);
    }

    //@Bean 또는 @Configuration 어노테이션이 해당 클래스/메서드에 존재하면, 해당 빈은 파싱의 대상이 됨
    private Set<BeanDefinition> findConfigBeanDefinition(BeanDefinitionRegistry registry) {
        return registry.getBeanDefinitions().stream()
            .filter(bd ->
                    (hasBeanAnnotation(bd.getBeanClass()) || hasComponentScanAnnotation(bd.getBeanClass())))
                .collect(Collectors.toSet());
    }

    private boolean hasComponentScanAnnotation(Class<?> beanClass) {
        return beanClass.isAnnotationPresent(ComponentScan.class);
    }

    private boolean hasBeanAnnotation(Class<?> beanClass) {
        return Arrays.stream(beanClass.getDeclaredMethods())
                .anyMatch(method -> method.isAnnotationPresent(Bean.class));
    }
}
