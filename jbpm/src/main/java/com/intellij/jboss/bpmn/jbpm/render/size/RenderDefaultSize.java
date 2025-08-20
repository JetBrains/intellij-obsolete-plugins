package com.intellij.jboss.bpmn.jbpm.render.size;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@RenderDefaultSize(width = 48, height = 48)
public @interface RenderDefaultSize {
  double width();

  double height();
}
