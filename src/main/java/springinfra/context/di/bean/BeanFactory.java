package springinfra.context.di.bean;

public interface BeanFactory {
    Object getBean(String beanName);

    <T> T getBean(Class<T> requireType);
}
