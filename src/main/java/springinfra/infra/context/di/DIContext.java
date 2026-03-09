package springinfra.infra.context.di;

import static springinfra.util.logger.Logger.log;

import springinfra.infra.context.di.bdrpp.BeanDefinitionRegistryPostProcessor;
import springinfra.infra.context.di.bdrpp.BeanFactoryPostProcessor;
import springinfra.infra.context.di.bdrpp.BeanNameGenerator;
import springinfra.infra.context.di.bean.DefaultBeanFactory;
import springinfra.infra.context.di.beandef.BeanDefinition;
import springinfra.infra.context.di.beandef.RootBeanDefinition;
import springinfra.infra.context.di.beandef.RootBeanDefinition.BeanDefinitionType;
import springinfra.infra.context.di.bpp.BeanPostProcessor;
import springinfra.infra.context.di.util.AnnotationConfigUtils;

import java.util.List;

public class DIContext {
    private final DefaultBeanFactory beanFactory;
    private final BeanNameGenerator beanNameGenerator;

    public DIContext(Class<?> appClass) {
        this.beanFactory = new DefaultBeanFactory();
        this.beanNameGenerator = new BeanNameGenerator();

        AnnotationConfigUtils.registerMainClass(
                beanFactory, appClass, beanNameGenerator.generateBeanName(appClass, beanFactory));
        AnnotationConfigUtils.registerInfraBeanDefinition(beanFactory);
    }

    public void refresh() {
        prepareRefresh();

        invokeBeanFactoryPostProcessors();

        registerBeanPostProcessors();

        finishBeanFactoryInitialization();

        finishRefresh();
    }

    private void prepareRefresh() {
        log("DIContext 준비 시작");
    }

    private void invokeBeanFactoryPostProcessors() {
        List<BeanDefinitionRegistryPostProcessor> bdrpps = beanFactory.getBeanListOfType(BeanDefinitionRegistryPostProcessor.class);
        bdrpps.forEach(bdrpp -> bdrpp.postProcessBeanDefinitionRegistry(beanFactory));

        List<BeanFactoryPostProcessor> bfpps = beanFactory.getBeanListOfType(BeanFactoryPostProcessor.class);
        bfpps.forEach(bfpp -> bfpp.postProcessBeanFactory(beanFactory));
    }

    private void registerBeanPostProcessors() {
        List<BeanPostProcessor> bpps = beanFactory.getBeanListOfType(BeanPostProcessor.class);
        bpps.forEach(beanFactory::addBeanPostProcessor);
    }

    private void finishBeanFactoryInitialization() {
        for(BeanDefinition beanDefinition : beanFactory.getBeanDefinitions()) {
            if(beanDefinition instanceof RootBeanDefinition rbd) {
                if(rbd.getBeanDefinitionType() == BeanDefinitionType.INFRA) continue;
            }
            beanFactory.getBean(beanDefinition.getBeanName());
        }
    }

    private void finishRefresh() {
        log("DIContext 끝");
        log(beanFactory.toString());
    }
}
