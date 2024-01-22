package com.intellij.jboss.bpmn.jbpm.render.background;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface RenderBorder {
  int width();

  int[] rgba() default {255, 255, 255, 255};

  int[] rgbaDark() default {255, 255, 255, 255};
}
