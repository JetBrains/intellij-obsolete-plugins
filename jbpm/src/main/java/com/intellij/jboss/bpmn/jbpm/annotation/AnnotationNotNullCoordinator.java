package com.intellij.jboss.bpmn.jbpm.annotation;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;

public class AnnotationNotNullCoordinator<T extends Annotation> extends AnnotationCoordinator<T> {
  @NotNull private final T defaultValue;

  public AnnotationNotNullCoordinator(@NotNull Class<T> annotationClass, @NotNull T value) {
    super(annotationClass);
    defaultValue = value;
  }

  @NotNull
  @Override
  public T getAnnotation(Class<?> clazz) {
    T value = super.getAnnotation(clazz);
    return value == null ? defaultValue : value;
  }

  @NotNull
  @Override
  public T getAnnotation(Iterable<Class<?>> classes) {
    T value = super.getAnnotation(classes);
    return value == null ? defaultValue : value;
  }
}
