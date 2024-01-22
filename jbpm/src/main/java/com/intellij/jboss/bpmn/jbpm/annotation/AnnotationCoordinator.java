package com.intellij.jboss.bpmn.jbpm.annotation;

import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.*;

public class AnnotationCoordinator<T extends Annotation> {
  @NotNull final private Class<T> annotationClass;
  private final Map<Set<Class<?>>, T> cache = new HashMap<>();

  public AnnotationCoordinator(@NotNull Class<T> annotationClass) {
    this.annotationClass = annotationClass;
  }

  public AnnotationCoordinator(@NotNull Class<T> annotationClass, @NotNull T defaultValue) {
    this.annotationClass = annotationClass;
  }

  public T getAnnotation(final Iterable<Class<?>> classes) {
    Set<Class<?>> set = ContainerUtil.newHashSet(classes);
    if (cache.containsKey(set)) {
      return cache.get(set);
    }
    T result = getAnnotationImpl(classes);
    cache.put(set, result);
    return result;
  }

  private T getAnnotationImpl(final Iterable<Class<?>> classes) {

    List<T> annotations = JBIterable.from(classes)
      .transform(aClass -> getAnnotationImpl(aClass))
      .filter(t -> t != null)
      .toList();
    if (annotations.size() == 0) {
      return null;
    }
    T result = annotations.get(0);
    for (T annotation : JBIterable.from(annotations).skip(1)) {
      assert annotation.equals(result) : "Found multiple annotations with type " +
                                         annotationClass.getName() +
                                         " and different values; you should annotate child class too to avoid this warning";
    }
    return result;
  }

  public T getAnnotation(Class<?> clazz) {
    Set<Class<?>> set = Collections.singleton(clazz);
    if (cache.containsKey(set)) {
      return cache.get(set);
    }
    T result = getAnnotationImpl(clazz);
    cache.put(set, result);
    return result;
  }

  private T getAnnotationImpl(Class<?> clazz) {
    T annotation = clazz.getAnnotation(annotationClass);
    if (annotation != null) {
      return annotation;
    }
    return getAnnotationImpl(JBIterable.<Class<?>>of(clazz.getSuperclass()).append(clazz.getInterfaces()));
  }
}
