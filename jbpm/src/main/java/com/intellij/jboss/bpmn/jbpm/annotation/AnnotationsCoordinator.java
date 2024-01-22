package com.intellij.jboss.bpmn.jbpm.annotation;

import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.*;

public final class AnnotationsCoordinator {
  private final Set<Class<? extends Annotation>> annotationsSet;

  public AnnotationsCoordinator(Collection<Class<? extends Annotation>> annotations) {
    this.annotationsSet = new HashSet<>(annotations);
  }

  public AnnotationsCoordinator(Class<? extends Annotation> annotation) {
    this.annotationsSet = new HashSet<>();
    annotationsSet.add(annotation);
  }

  public Map<Class<? extends Annotation>, Annotation> findAnnotations(final Iterable<Class<?>> classes) {
    return findAnnotations(classes, Collections.emptySet());
  }

  private Map<Class<? extends Annotation>, Annotation> findAnnotations(final Iterable<Class<?>> classes,
                                                                       final Set<Class<? extends Annotation>> excludedAnnotations) {
    final Map<Class<? extends Annotation>, Annotation> annotationMap = new HashMap<>();
    List<Map.Entry<Class<? extends Annotation>, Annotation>> annotations = JBIterable.from(classes)
      .filter(aClass -> aClass != null)
      .transform(aClass -> findAnnotations(aClass, excludedAnnotations))
      .flatten(map -> map.entrySet()).toList();
    for (Map.Entry<Class<? extends Annotation>, Annotation> entry : annotations) {
      assert !annotationAlreadyExistAndDiffer(annotationMap, entry.getValue())
        : "Found multiple annotations with type " +
          entry.getKey().getName() +
          " and different values; you should annotate child class too to avoid this warning";
      annotationMap.put(entry.getKey(), entry.getValue());
    }
    return annotationMap;
  }

  private Map<Class<? extends Annotation>, Annotation> findAnnotations(final Class<?> clazz,
                                                                       Set<Class<? extends Annotation>> excludedAnnotations) {
    final Map<Class<? extends Annotation>, Annotation> annotations = new HashMap<>();
    for (Annotation annotation : clazz.getAnnotations()) {
      Class<? extends Annotation> annotationClass = annotation.annotationType();
      if (excludedAnnotations.contains(annotationClass) || !annotationsSet.contains(annotationClass)) {
        continue;
      }
      annotations.put(annotationClass, annotation);
    }

    final Set<Class<? extends Annotation>> parentExcludedAnnotations =
      new HashSet<>(excludedAnnotations);
    parentExcludedAnnotations.addAll(annotations.keySet());

    Map<Class<? extends Annotation>, Annotation> parentAnnotations = findAnnotations(
      JBIterable.<Class<?>>of(clazz.getSuperclass()).append(clazz.getInterfaces()),
      parentExcludedAnnotations);

    annotations.putAll(parentAnnotations);
    return annotations;
  }

  private static boolean annotationAlreadyExistAndDiffer(@NotNull Map<Class<? extends Annotation>, Annotation> annotations,
                                                         @NotNull Annotation annotation) {
    Annotation existAnnotation = annotations.get(annotation.annotationType());
    return existAnnotation != null && !existAnnotation.equals(annotation);
  }
}
