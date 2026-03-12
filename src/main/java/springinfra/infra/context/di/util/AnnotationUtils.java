package springinfra.infra.context.di.util;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

public class AnnotationUtils {
    public static boolean hasAnnotation(Class<?> beanClass, Class<?> target) {
        Set<Class<?>> visited = new HashSet<>();

        for(Annotation annotation : beanClass.getAnnotations()) {
            if(visited.contains(annotation.annotationType())) continue;
            if(findAnnotation(annotation, target, visited)) return true;
        }
        return false;
    }

    private static boolean findAnnotation(Annotation annotation, Class<?> target, Set<Class<?>> visited) {
        Class<?> annotationClass = annotation.annotationType();
        if(annotationClass == target) return true;

        for(Annotation childAnnotation : annotationClass.getAnnotations()) {
            if(visited.contains(childAnnotation.annotationType())) continue;

            visited.add(childAnnotation.annotationType());
            if(findAnnotation(childAnnotation, target, visited)) return true;
        }
        return false;
    }
}
