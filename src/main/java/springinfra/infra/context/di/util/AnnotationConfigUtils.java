package springinfra.infra.context.di.util;

import springinfra.infra.context.di.bdrpp.ConfigurationClassPostProcessor;
import springinfra.infra.context.di.beandef.BeanDefinitionRegistry;
import springinfra.infra.context.di.beandef.ConfigurationBeanDefinition;
import springinfra.infra.context.di.beandef.RootBeanDefinition;
import springinfra.infra.context.di.bpp.aop.AutoProxyCreator;

import static springinfra.infra.context.di.beandef.RootBeanDefinition.BeanDefinitionType.*;

public class AnnotationConfigUtils {
    private final static String CCPP_NAME = "configurationClassPostProcessor";
    private final static String APC = "autoProxyCreator";

    public static void registerMainClass(BeanDefinitionRegistry bdr, Class<?> mainClass, String name) {
        bdr.registerBeanDefinition(
                name,
                new ConfigurationBeanDefinition(name, mainClass)
        );
    }

    public static void registerInfraBeanDefinition(BeanDefinitionRegistry bdr) {
        registerConfigurationClassPostProcessor(bdr);
        registerAutoProxyCreator(bdr);
    }

    private static void registerConfigurationClassPostProcessor(BeanDefinitionRegistry bdr) {
        bdr.registerBeanDefinition(
                CCPP_NAME,
                new RootBeanDefinition(INFRA, CCPP_NAME, ConfigurationClassPostProcessor.class)
        );
    }

    private static void registerAutoProxyCreator(BeanDefinitionRegistry bdr) {
        bdr.registerBeanDefinition(
                APC,
                new RootBeanDefinition(INFRA, APC, AutoProxyCreator.class)
        );
    }
}
