package springinfra.context.di.bean;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import springinfra.annotation.Autowired;
import springinfra.context.di.beandef.BeanDefinition;
import springinfra.context.di.beandef.BeanDefinitionRegistry;
import springinfra.context.di.beandef.MethodBeanDefinition;

public class DefaultBeanFactory implements BeanDefinitionRegistry, BeanFactory {
    private final Map<String, BeanDefinition> beanDefinitions;
    private final Map<String, Object> singletonBeans;
    private final Set<String> singletonsCurrentlyCreation;

    public DefaultBeanFactory() {
        this.beanDefinitions = new LinkedHashMap<>(256);
        this.singletonBeans = new LinkedHashMap<>(256);
        this.singletonsCurrentlyCreation = new LinkedHashSet<>();
    }

    //Bean Definition Registry=======================================
    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) {
        if(beanDefinitions.containsKey(beanName)) {
            throw new IllegalStateException("동일한 이름의 빈이 이미 존재합니다. : " + beanName);
        }
        this.beanDefinitions.put(beanName, beanDefinition);
    }

    @Override
    public boolean containsBeanDefinition(String beanName) {
        return beanDefinitions.containsKey(beanName);
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) {
        if(!containsBeanDefinition(beanName)) {
            throw new IllegalStateException("해당 BeanDefinition이 없습니다. : " + beanName);
        }
        return beanDefinitions.get(beanName);
    }

    @Override
    public Set<BeanDefinition> getBeanDefinitions() {
        return new LinkedHashSet<>(beanDefinitions.values());
    }

    @Override
    public <T extends BeanDefinition> Set<T> getBeanDefinitions(Class<T> beanDefinitionType) {
        return beanDefinitions.values().stream()
            .filter(beanDefinitionType::isInstance)
            .map(beanDefinitionType::cast)
            .collect(Collectors.toSet());
    }

    //Bean Factory===============================================
    @Override
    public Object getBean(String beanName) {
        if(singletonBeans.containsKey(beanName)) {
            return singletonBeans.get(beanName);
        }
        if(!beanDefinitions.containsKey(beanName)) {
            throw new IllegalStateException("해당 Bean이 없습니다. : " + beanName);
        }
        return getBeanPipeLine(beanName, beanDefinitions.get(beanName));
    }

    @Override
    public <T> T getBean(Class<T> requireType) {
        return requireType.cast(getBean(requireType.getName()));
    }

    private Object getBeanPipeLine(String beanName, BeanDefinition beanDefinition) {
        beforeSingletonCreation(beanName);

        Object beanInstance = createBean(beanName, beanDefinition);

        afterSingletonCreation(beanName);

        return beanInstance;
    }

    private void beforeSingletonCreation(String beanName) {
        if(this.singletonsCurrentlyCreation.contains(beanName)) {
            throw new IllegalStateException("순환 참조 발생!");
        }
        singletonsCurrentlyCreation.add(beanName);
    }

    private Object createBean(String beanName, BeanDefinition beanDefinition) {
        Object beanInstance = createBeanInstance(beanDefinition);

        //populateBean(beanName, beanDefinition); ==> setter 기반 의존주입 시

        //initializeBean(beanName, beanDefinition, beanInstance); BeanPostProcessor 전/후처리 부분(AOP 등), 초기화 메서드 등

        return beanInstance;
    }

    private Object createBeanInstance(BeanDefinition beanDefinition) {
        if(beanDefinition instanceof MethodBeanDefinition methodBeanDefinition) {
            return createMethodBeanInstance(methodBeanDefinition);
        }
        return createClassBeanInstance(beanDefinition);
    }

    private Object createMethodBeanInstance(MethodBeanDefinition methodBeanDefinition) {
        return null;
    }

    private Object createClassBeanInstance(BeanDefinition beanDefinition) {
        Class<?> beanClass = beanDefinition.getBeanClass();
        Constructor<?> autowirableConstructor = selectAutowirableConstructor(beanClass.getDeclaredConstructors());
        Object[] constructorParameters = null;
        try{
            constructorParameters = resolveParameters(autowirableConstructor);
            return autowirableConstructor.newInstance(constructorParameters);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("빈 생성 예외 발생(추후 처리)");
        }
    }

    private Constructor<?> selectAutowirableConstructor(Constructor<?>[] declaredConstructors) {
        if(declaredConstructors.length == 1) {
            return declaredConstructors[0];
        }

        Constructor<?> noArgsConstructor = null;
        for(Constructor<?> constructor : declaredConstructors) {
            if(constructor.isAnnotationPresent(Autowired.class)) {
                return constructor;
            }
            if(constructor.getParameterCount() == 0) noArgsConstructor = constructor;
        }

        if(noArgsConstructor == null) {
            throw new IllegalStateException("의존주입에 사용할 수 있는 생성자를 찾을 수 없습니다.");
        }
        return noArgsConstructor;
    }

    private Object[] resolveParameters(Constructor<?> autowirableConstructor) {
        Class<?>[] parameterTypes = autowirableConstructor.getParameterTypes();
        int parameterCount = autowirableConstructor.getParameterCount();
        Object[] parameters = new Object[parameterCount];
        for(int i = 0; i < parameterCount; i++) {
            parameters[i] = getBean(parameterTypes[i]);
        }
        return parameters;
    }

    private void afterSingletonCreation(String beanName) {
        if(!this.singletonsCurrentlyCreation.contains(beanName)) {
            throw new IllegalStateException("빈 생성 중 알 수 없는 예외 발생(꼬임)");
        }
        singletonsCurrentlyCreation.remove(beanName);
    }
}
