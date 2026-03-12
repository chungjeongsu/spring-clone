package springinfra.infra.context.di.bean;

import java.util.List;
import java.util.Map;
import springinfra.infra.context.di.beandef.BeanDefinition;

public interface BeanFactory {
    Object getBean(String beanName);

    <T> T getBean(Class<T> requireType);

    <T> Map<String, T> getBeanMapOfType(Class<T> type);

    <T> List<T> getBeanListOfType(Class<T> type);

    boolean hasBean(String beanName);

    <T> boolean hasBean(Class<T> type);

    void registerSingletonBean(String beanName, Object beanInstance);

    <T> BeanDefinition getBeanDefinition(T type);
}
