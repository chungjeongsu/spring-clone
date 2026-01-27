package springinfra.context.di;

import static util.logger.Logger.log;

import springinfra.context.di.bdrpp.BeanDefinitionRegistryPostProcessor;
import springinfra.context.di.bdrpp.BeanFactoryPostProcessor;
import springinfra.context.di.bdrpp.BeanNameGenerator;
import springinfra.context.di.bdrpp.ConfigurationClassPostProcessor;
import springinfra.context.di.bean.DefaultBeanFactory;
import springinfra.context.di.beandef.BeanDefinition;
import springinfra.context.di.beandef.RootBeanDefinition;
import springinfra.context.di.beandef.RootBeanDefinition.BeanDefinitionType;
import springinfra.context.di.bpp.BeanPostProcessor;

import java.util.List;

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
            beanName, new RootBeanDefinition(BeanDefinitionType.INFRA, beanName, appClass)
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

    /**
     * BeanDefinitionRegisterPostProcessor의 BeanDefinition을 BeanFactory에 등록한다.
     * - ConfigurationClassPostProcessor : 클래스 패스를 스캔하여, BeanDefinition을 등록하는 역할
     */
    private void registerInfrastructureProcessors() {
        registerBasicBDRPPDefinition();
    }

    /**
     *  등록된 BeanDefinitionRegisterPostProcessor를 실행한다.
     *  - ConfigurationClassPostProcessor : 대부분의 빈의 BeanDefinition이 여기에서 등록된다.
     */
    private void invokeBeanFactoryPostProcessors() {
        List<BeanDefinitionRegistryPostProcessor> bdrpps = beanFactory.getBeanListOfType(
            BeanDefinitionRegistryPostProcessor.class);

        for(BeanDefinitionRegistryPostProcessor bdrpp : bdrpps) {
            bdrpp.postProcessorBeanDefinitionRegistry(beanFactory);
        }

        List<BeanFactoryPostProcessor> bfpps = beanFactory.getBeanListOfType(
            BeanFactoryPostProcessor.class);

        for(BeanFactoryPostProcessor bfpp : bfpps) {
            bfpp.postProcessBeanFactory(beanFactory, beanFactory);
        }
    }

    /**
     * invokeBeanFactoryPostProcessors에서 등록된 BeanPostProcessor의 BeanDefinition들을 통해, BeanPostProcessor 빈을 등록한다.
     * - BeanFactory 내부의 BeanPostProcessor만을 모아두는 List에 저장한다.
     * - 후에 빈들을 생성할 때( createBean() ), BeanPostProcessor가 수행된다.
     */
    private void registerBeanPostProcessors() {
        List<BeanPostProcessor> bpps = beanFactory.getBeanListOfType(BeanPostProcessor.class);
        for (BeanPostProcessor bpp : bpps) {
            beanFactory.addBeanPostProcessor(bpp);
        }
    }

    /**
     * 모든 필요한 빈들을 생성한다.
     */
    private void finishBeanFactoryInitialization() {
        for(BeanDefinition beanDefinition : beanFactory.getBeanDefinitions()) {
            if(beanDefinition instanceof RootBeanDefinition rbd) {
                if(rbd.getBeanDefinitionType() == BeanDefinitionType.INFRA) continue;
            }
            beanFactory.getBean(beanDefinition.getBeanName());
        }
    }

    private void finishRefresh() {

    }

    private void registerBasicBDRPPDefinition() {
        String beanName = beanNameGenerator.generateBeanName(ConfigurationClassPostProcessor.class, beanFactory);
        beanFactory.registerBeanDefinition(
            beanName, new RootBeanDefinition(
                BeanDefinitionType.INFRA,
                beanName,
                ConfigurationClassPostProcessor.class
            )
        );
    }
}
