package springinfra.context.di;

import static util.logger.Logger.log;

import springinfra.context.di.bdrpp.BeanDefinitionRegistryPostProcessor;
import springinfra.context.di.bdrpp.BeanFactoryPostProcessor;
import springinfra.context.di.bdrpp.BeanNameGenerator;
import springinfra.context.di.bean.DefaultBeanFactory;
import springinfra.context.di.beandef.BeanDefinition;
import springinfra.context.di.beandef.RootBeanDefinition;
import springinfra.context.di.beandef.RootBeanDefinition.BeanDefinitionType;
import springinfra.context.di.bpp.BeanPostProcessor;
import springinfra.context.di.util.AnnotationConfigUtils;

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
        bfpps.forEach(bfpp -> bfpp.postProcessBeanFactory(beanFactory, beanFactory));
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
    }
}
