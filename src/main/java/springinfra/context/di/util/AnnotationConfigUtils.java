package springinfra.context.di.util;

import springinfra.context.di.bdrpp.ConfigurationClassPostProcessor;
import springinfra.context.di.beandef.BeanDefinitionRegistry;
import springinfra.context.di.beandef.ConfigurationBeanDefinition;
import springinfra.context.di.beandef.RootBeanDefinition;

import static springinfra.context.di.beandef.RootBeanDefinition.BeanDefinitionType.*;

public class AnnotationConfigUtils {
    private final static String CCPP_NAME = "configurationClassPostProcessor";

    public static void registerMainClass(BeanDefinitionRegistry bdr, Class<?> mainClass, String name) {
        bdr.registerBeanDefinition(
                name,
                new ConfigurationBeanDefinition(name, mainClass)
        );
    }

    public static void registerInfraBeanDefinition(BeanDefinitionRegistry bdr) {
        registerConfigurationClassPostProcessor(bdr);
    }

    private static void registerConfigurationClassPostProcessor(BeanDefinitionRegistry bdr) {
        bdr.registerBeanDefinition(
                CCPP_NAME,
                new RootBeanDefinition(INFRA, CCPP_NAME, ConfigurationClassPostProcessor.class)
        );
    }
}
