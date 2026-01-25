package springinfra.context.di.bean;

import java.util.List;
import java.util.Map;

public interface BeanFactory {
    Object getBean(String beanName);

    <T> T getBean(Class<T> requireType);

    <T> Map<String, T> getBeanMapOfType(Class<T> type);

    <T> List<T> getBeanListOfType(Class<T> type);
}
