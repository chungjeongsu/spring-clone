package springinfra.context.di;

import static util.logger.Logger.log;

import springinfra.context.di.bdrpp.BeanNameGenerator;
import springinfra.context.di.bdrpp.ConfigurationClassPostProcessor;
import springinfra.context.di.bean.DefaultBeanFactory;
import springinfra.context.di.beandef.RootBeanDefinition;
import springinfra.context.di.beandef.RootBeanDefinition.BeanDefinitionType;

/**
 * 의존 주입의 역할을 맡는 컨텍스트이다.
 * - refresh()를 통해 빈들을 등록한다.
 *
 * 1. prepareRefresh()
 * : 준비
 *
 * 2. registerInfrastructureProcessors()
 * : BFPP, BDRPP 빈들을 먼저 싱글톤으로 등록
 *
 * 3. invokeBeanFactoryPostProcessors()
 * : BFPP, BDRPP를 통해 빈 정의를 확장/수정/추가
 *
 * 4. registerBeanPostProcessors()
 * : BPP를 싱글톤으로 등록
 *
 * 5. finishBeanFactoryInitialization()
 * : 싱글톤 빈 인스턴스들 생성/주입/초기화
 *
 * 6. finishRefresh()
 * : 마무리
 */
public class DIContext {
    private final DefaultBeanFactory beanFactory;
    private final BeanNameGenerator beanNameGenerator;

    public DIContext(Class<?> appClass) {
        this.beanFactory = new DefaultBeanFactory();
        this.beanNameGenerator = new BeanNameGenerator();
        registerAppBeanDefinition(appClass);
    }

    private void registerAppBeanDefinition(Class<?> appClass) {
        String beanName = beanNameGenerator.generateBeanName(appClass, beanFactory);
        beanFactory.registerBeanDefinition(
            beanName,
            new RootBeanDefinition(
                BeanDefinitionType.INFRA, beanName, appClass
            )
        );
    }

    public void refresh() {
        prepareRefresh();

        registerInfrastructureProcessors();

        invokeBeanFactoryPostProcessors();

        registerBeanPostProcessors();

        finishBeanFactoryInitialization();

        finishRefresh();
    }

    private void prepareRefresh() {
        log("DIContext 준비 시작");
    }

    private void registerInfrastructureProcessors() {   //BDRPP 등록
        String beanName = beanNameGenerator.generateBeanName(
            ConfigurationClassPostProcessor.class, beanFactory
        );
        beanFactory.registerBeanDefinition(
            beanName,
            new RootBeanDefinition(
                BeanDefinitionType.INFRA,
                beanName,
                ConfigurationClassPostProcessor.class
            )
        );
    }

    private void invokeBeanFactoryPostProcessors() {

    }

    private void registerBeanPostProcessors() {

    }

    private void finishBeanFactoryInitialization() {

    }

    private void finishRefresh() {

    }
}
