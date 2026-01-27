package springinfra.context.di.bean;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import springinfra.annotation.Autowired;
import springinfra.context.di.beandef.BeanDefinition;
import springinfra.context.di.beandef.BeanDefinitionRegistry;
import springinfra.context.di.beandef.MethodBeanDefinition;
import springinfra.context.di.bpp.BeanPostProcessor;

public class DefaultBeanFactory implements BeanDefinitionRegistry, BeanFactory {
    private final Map<String, BeanDefinition> beanDefinitions;
    private final Map<String, Object> singletonBeans;
    private final Set<String> singletonsCurrentlyCreation;
    private final Map<Class<?>, Set<String>> typeIndex; //인터페이스, 상속 구조
    private final List<BeanPostProcessor> beanPostProcessors;

    public DefaultBeanFactory() {
        this.beanDefinitions = new LinkedHashMap<>(256);
        this.singletonBeans = new LinkedHashMap<>(256);
        this.singletonsCurrentlyCreation = new LinkedHashSet<>();
        this.typeIndex = new LinkedHashMap<>();
        this.beanPostProcessors = new ArrayList<>();
    }

    //Bean Definition Registry=======================================
    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) {
        if(beanDefinitions.containsKey(beanName)) {
            throw new IllegalStateException("동일한 이름의 빈이 이미 존재합니다. : " + beanName);
        }

        this.beanDefinitions.put(beanName, beanDefinition);

        Set<Class<?>> superTypes = getSuperTypes(beanDefinition.getBeanClass());
        for(Class<?> superType : superTypes) {
            typeIndex.computeIfAbsent(superType, values -> new LinkedHashSet<>())
                    .add(beanName);
        }
    }

    private Set<Class<?>> getSuperTypes(Class<?> beanClass) {
        Set<Class<?>> superTypes = new LinkedHashSet<>();

        Class<?> curr = beanClass;
        while(curr != Object.class && curr != null) {
            superTypes.add(curr);
            collectInterfaces(curr, superTypes);
            curr = curr.getSuperclass();
        }
        return superTypes;
    }

    private void collectInterfaces(Class<?> curr, Set<Class<?>> superTypes) {
        for(Class<?> itf : curr.getInterfaces()) {
            if(superTypes.add(itf)) {
                collectInterfaces(itf, superTypes);
            }
        }
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

    //todo : 현재는 1번째 후보만 가져오고 있음. 추후에 qualifier 로직 추가 예정
    @Override
    public <T> T getBean(Class<T> requireType) {
        Set<String> candidateNames = typeIndex.get(requireType);
        String candidateName = candidateNames.iterator().next();
        return requireType.cast(getBean(candidateName));
    }

    @Override
    public <T> Map<String, T> getBeanMapOfType(Class<T> type) {
        Map<String, T> typeBeans = new LinkedHashMap<>();
        for(String beanName : typeIndex.get(type)) {
            typeBeans.put(beanName, type.cast(getBean(beanName)));
        }
        if(typeBeans.isEmpty()) throw new IllegalStateException("해당 타입의 빈이 없습니다! : " + type.getName());
        return typeBeans;
    }

    @Override
    public <T> List<T> getBeanListOfType(Class<T> type) {
        List<T> typeBeans = new ArrayList<>();
        for(String beanName : typeIndex.get(type)) {
            typeBeans.add(type.cast(getBean(beanName)));
        }
        if(typeBeans.isEmpty()) throw new IllegalStateException("해당 타입의 빈이 없습니다! : " + type.getName());
        return typeBeans;
    }

    @Override
    public <T> boolean hasBean(Class<T> type) {
        return hasBean(type.getName());
    }

    @Override
    public boolean hasBean(String beanName) {
        return beanDefinitions.containsKey(beanName);
    }

    @Override
    public void registerSingletonBean(String beanName, Object beanInstance) {
        if(singletonBeans.containsKey(beanName))
            throw new IllegalStateException("동일한 빈이 존재합니다. : " + beanName);
        singletonBeans.put(beanName, beanInstance);
    }

    @Override
    public <T> BeanDefinition getBeanDefinition(T type) {
        return getBeanDefinition(type.getClass().getName());
    }

    public void addBeanPostProcessor(BeanPostProcessor bpp) {
        this.beanPostProcessors.add(bpp);
    }

    private Object getBeanPipeLine(String beanName, BeanDefinition beanDefinition) {
        beforeSingletonCreation(beanName);

        Object beanInstance = createBean(beanName, beanDefinition);

        singletonBeans.put(beanName, beanInstance);

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

    //todo: 메서드 빈 생성 오버로드 리팩터링 필요
    private Object createBeanInstance(BeanDefinition beanDefinition) {
        if(beanDefinition instanceof MethodBeanDefinition methodBeanDefinition) {
            return createMethodBeanInstance(methodBeanDefinition);  //method 오버로드 시 터짐->오버로드 없게하기
        }
        return createClassBeanInstance(beanDefinition);
    }

    private Object createMethodBeanInstance(MethodBeanDefinition methodBeanDefinition) {
        String factoryBeanName = methodBeanDefinition.getFactoryBeanName();
        Object factoryBean = singletonBeans.get(factoryBeanName);
        if(factoryBean == null) {
            factoryBean = getBean(factoryBeanName);
        }
        String factoryMethodName = methodBeanDefinition.getFactoryMethod();
        Method factoryMethod = findFactoryMethod(factoryBean.getClass(), factoryMethodName);
        Object[] factoryMethodParameters = getFactoryMethodParameterBeans(factoryMethod);

        try{
            return factoryMethod.invoke(factoryBean, factoryMethodParameters);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new IllegalStateException("method bean 생성 중 예외 발생 : " + factoryBeanName);
        }
    }

    private Object[] getFactoryMethodParameterBeans(Method factoryMethod) {
        return Arrays.stream(factoryMethod.getParameters())
                .map(parameter -> getBean(parameter.getType()))
                .toArray();
    }

    private Method findFactoryMethod(Class<?> factoryBeanClass, String factoryMethodName) {
        return Arrays.stream(factoryBeanClass.getDeclaredMethods())
                .filter(method -> method.getName().equals(factoryMethodName))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("해당 bean을 생성할 수 있는 FactoryMethod가 없습니다. : " + factoryMethodName));
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
