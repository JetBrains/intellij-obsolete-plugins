package com.intellij.lang.puppet;

import com.intellij.lang.puppet.project.PuppetTestCase;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that controls the set of puppet language versions for which test class/methods should be run.
 * Works effectively only if used with descendants of {@link PuppetTestCase}.
 * @see PuppetTestCase
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
public @interface OnVersion {
  /**
   * Language versions for which the annotated class/method should be run.
   * @return an array containing the versions needed
   */
  PuppetLanguage.Version[] value();
}
