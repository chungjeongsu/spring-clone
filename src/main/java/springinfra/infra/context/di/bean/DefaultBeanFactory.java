package springinfra.infra.context.di.bean;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Factory;
import net.sf.cglib.proxy.NoOp;
import springinfra.infra.annotation.bean.Autowired;
import springinfra.infra.context.di.bdrpp.FactoryMethodInterceptor;
import springinfra.infra.context.di.beandef.BeanDefinition;
import springinfra.infra.context.di.beandef.BeanDefinitionRegistry;
import springinfra.infra.context.di.beandef.MethodBeanDefinition;
import springinfra.infra.context.di.beandef.RootBeanDefinition;
import springinfra.infra.context.di.bpp.BeanPostProcessor;
import springinfra.infra.context.di.exception.BeanDefinitionException;
import springinfra.infra.context.di.exception.BeanResolveException;

public class DefaultBeanFactory implements BeanDefinitionRegistry, BeanFactory {
    private static final String INTERNAL_BEAN_FACTORY_NAME = "__internalDefaultBeanFactory";
    private final Map<String, BeanDefinition> beanDefinitions;
    private final Map<String, Object> singletonBeans;
    private final Set<String> singletonsCurrentlyCreation;
    private final Map<Class<?>, Set<String>> typeIndex; // 타입별 빈 이름 인덱스
    private final Map<Class<?>, Set<Advice>> adviceIndex;
    private final List<BeanPostProcessor> beanPostProcessors;

    public DefaultBeanFactory() {
        this.beanDefinitions = new LinkedHashMap<>(256);
        this.singletonBeans = new LinkedHashMap<>(256);
        this.singletonsCurrentlyCreation = new LinkedHashSet<>();
        this.typeIndex = new LinkedHashMap<>();
        this.beanPostProcessors = new ArrayList<>();
        this.adviceIndex = new LinkedHashMap<>();
        registerSingletonBean(INTERNAL_BEAN_FACTORY_NAME, this);
    }

    //Bean Definition Registry=======================================
    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) {
        if(beanDefinitions.containsKey(beanName)) {
            throw new BeanDefinitionException("동일한 이름의 빈 정의가 이미 존재합니다. : " + beanName);
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

    //todo : 현재는 1번째 후보만 가져오고 있음. 추후 qualifier 로직 추가 예정
    @Override
    public <T> T getBean(Class<T> requireType) {
        Set<String> candidateNames = resolveCandidateNames(requireType);
        if(candidateNames.isEmpty()) {
            throw new BeanResolveException("해당 타입의 빈이 없습니다. : " + requireType.getName());
        }
        if(candidateNames.size() > 1) {
            throw new BeanResolveException("해당 타입의 빈이 2개 이상입니다. : "
                    + requireType.getName() + ", candidates=" + candidateNames);
        }
        String candidateName = candidateNames.iterator().next();
        return requireType.cast(getBean(candidateName));
    }

    @Override
    public <T> Map<String, T> getBeanMapOfType(Class<T> type) {
        Map<String, T> typeBeans = new LinkedHashMap<>();
        Set<String> candidateNames = resolveCandidateNames(type);
        for(String beanName : candidateNames) {
            typeBeans.put(beanName, type.cast(getBean(beanName)));
        }
        if(typeBeans.isEmpty()) {
            throw new BeanResolveException("해당 타입의 빈이 없습니다. : " + type.getName());
        }
        return typeBeans;
    }

    @Override
    public <T> List<T> getBeanListOfType(Class<T> type) {
        List<T> typeBeans = new ArrayList<>();
        Set<String> candidateNames = resolveCandidateNames(type);
        for(String beanName : candidateNames) {
            typeBeans.add(type.cast(getBean(beanName)));
        }
        if(typeBeans.isEmpty()) {
            throw new BeanResolveException("해당 타입의 빈이 없습니다. : " + type.getName());
        }
        return typeBeans;
    }

    @Override
    public <T> boolean hasBean(Class<T> type) {
        return !resolveCandidateNames(type).isEmpty();
    }

    @Override
    public boolean hasBean(String beanName) {
        return beanDefinitions.containsKey(beanName);
    }

    @Override
    public void registerSingletonBean(String beanName, Object beanInstance) {
        if(singletonBeans.containsKey(beanName))
            throw new IllegalStateException("duplicate singleton bean: " + beanName);
        singletonBeans.put(beanName, beanInstance);

        Set<Class<?>> superTypes = getSuperTypes(beanInstance.getClass());
        for(Class<?> superType : superTypes) {
            typeIndex.computeIfAbsent(superType, values -> new LinkedHashSet<>())
                    .add(beanName);
        }
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
        try {
            Object beanInstance = createBean(beanName, beanDefinition);
            singletonBeans.put(beanName, beanInstance);
            return beanInstance;
        } finally {
            afterSingletonCreation(beanName);
        }
    }

    private void beforeSingletonCreation(String beanName) {
        if(this.singletonsCurrentlyCreation.contains(beanName)) {
            throw new IllegalStateException("순환 참조 발생!");
        }
        singletonsCurrentlyCreation.add(beanName);
    }

    private Object createBean(String beanName, BeanDefinition beanDefinition) {
        Object beanInstance = createBeanInstance(beanDefinition);

        //populateBean(beanName, beanDefinition); ==> setter 기반 의존주입 처리
        return initializeBean(beanName, beanInstance);
    }

    private Object initializeBean(String beanName, Object beanInstance) {
        if(shouldSkipPostProcessing(beanName, beanInstance)) {
            return beanInstance;
        }

        Object wrappedBean = applyBeanPostProcessorsBeforeInitialization(beanInstance, beanName);
        return applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
    }

    private boolean shouldSkipPostProcessing(String beanName, Object bean) {
        if(bean instanceof BeanPostProcessor) {
            return true;
        }

        BeanDefinition beanDefinition = beanDefinitions.get(beanName);
        if(beanDefinition instanceof RootBeanDefinition rootBeanDefinition) {
            return rootBeanDefinition.getBeanDefinitionType() == RootBeanDefinition.BeanDefinitionType.INFRA;
        }

        return false;
    }

    private Object applyBeanPostProcessorsBeforeInitialization(Object bean, String beanName) {
        Object result = bean;
        for(BeanPostProcessor bpp : beanPostProcessors) {
            result = bpp.postProcessBeforeInitialization(result, beanName);
            if(result == null) {
                throw new IllegalStateException("BeanPostProcessor returned null in beforeInitialization: " + beanName);
            }
        }
        return result;
    }

    private Object applyBeanPostProcessorsAfterInitialization(Object bean, String beanName) {
        Object result = bean;
        for(BeanPostProcessor bpp : beanPostProcessors) {
            result = bpp.postProcessAfterInitialization(result, beanName);
            if(result == null) {
                throw new IllegalStateException("BeanPostProcessor returned null in afterInitialization: " + beanName);
            }
        }
        return result;
    }

    //todo: 메서드 빈 생성 오버로드 리팩토링 필요
    private Object createBeanInstance(BeanDefinition beanDefinition) {
        if(beanDefinition instanceof MethodBeanDefinition methodBeanDefinition) {
            return createMethodBeanInstance(methodBeanDefinition);  //method 오버로드 문제 지점 -> 오버로드 좁히기
        }
        return createClassBeanInstance(beanDefinition);
    }

    private Object createMethodBeanInstance(MethodBeanDefinition methodBeanDefinition) {
        String factoryBeanName = methodBeanDefinition.getFactoryBeanName();
        Object factoryBean = singletonBeans.get(factoryBeanName);
        if(factoryBean == null) {
            factoryBean = getBean(factoryBeanName);

            if(factoryBean instanceof Factory cglibProxy) {
                cglibProxy.setCallbacks(new Callback[]{new FactoryMethodInterceptor(this), NoOp.INSTANCE});
            }
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
                .orElseThrow(() -> new IllegalStateException("해당 bean을 생성하는 FactoryMethod가 없습니다. : " + factoryMethodName));
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
        Parameter[] parameters = autowirableConstructor.getParameters();
        Object[] resolved = new Object[parameters.length];

        for(int i = 0; i < parameters.length; i++) {
            resolved[i] = resolveParameter(parameters[i]);
        }
        return resolved;
    }

    private Object resolveParameter(Parameter parameter) {
        Class<?> rawType = parameter.getType();
        Type genericType = parameter.getParameterizedType();

        if(List.class.equals(rawType)) {
            Class<?> elementType = extractSingleGenericType(genericType, "List", parameter);
            return getBeanListOfType(elementType);
        }

        if(Map.class.equals(rawType)) {
            Class<?> valueType = extractMapValueType(genericType, parameter);
            return getBeanMapOfType(valueType);
        }

        return getBean(rawType);
    }

    private Class<?> extractSingleGenericType(Type genericType, String containerType, Parameter parameter) {
        if(!(genericType instanceof ParameterizedType parameterizedType)) {
            throw new BeanResolveException(containerType + " 주입은 제네릭 타입이 필요합니다. parameter=" + parameter.getName());
        }

        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        if(actualTypeArguments.length != 1) {
            throw new BeanResolveException(containerType + " 제네릭 타입이 올바르지 않습니다. parameter=" + parameter.getName());
        }

        if(!(actualTypeArguments[0] instanceof Class<?> parameterType)) {
            throw new BeanResolveException(containerType + " 제네릭은 Class 타입이어야 합니다. parameter=" + parameter.getName());
        }

        return parameterType;
    }

    private Class<?> extractMapValueType(Type genericType, Parameter parameter) {
        if(!(genericType instanceof ParameterizedType parameterizedType)) {
            throw new BeanResolveException("Map 주입은 제네릭 타입이 필요합니다. parameter=" + parameter.getName());
        }

        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        if(actualTypeArguments.length != 2) {
            throw new BeanResolveException("Map 제네릭 타입이 올바르지 않습니다. parameter=" + parameter.getName());
        }

        if(!(actualTypeArguments[0] instanceof Class<?> keyType) || !String.class.equals(keyType)) {
            throw new BeanResolveException("Map 주입 key 타입은 String 이어야 합니다. parameter=" + parameter.getName());
        }

        if(!(actualTypeArguments[1] instanceof Class<?> valueType)) {
            throw new BeanResolveException("Map 주입 value 타입은 Class 타입이어야 합니다. parameter=" + parameter.getName());
        }

        return valueType;
    }

    private Set<String> resolveCandidateNames(Class<?> type) {
        Set<String> names = typeIndex.get(type);
        if(names == null || names.isEmpty()) {
            return Collections.emptySet();
        }
        return new LinkedHashSet<>(names);
    }

    private void afterSingletonCreation(String beanName) {
        if(!this.singletonsCurrentlyCreation.contains(beanName)) {
            throw new IllegalStateException("빈 생성 중 예상하지 못한 예외가 발생했습니다.");
        }
        singletonsCurrentlyCreation.remove(beanName);
    }

    @Override
    public String toString() {
        return "BeanDefinitions==============================\n" + beanDefinitions
                + "\nSingletonBean===============================\n" + singletonBeans
                + "\nBeanPostProcessor==============================\n" + beanPostProcessors;
    }
}


