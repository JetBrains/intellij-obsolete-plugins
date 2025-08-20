package com.intellij.jboss.bpmn.jbpm.render.background;

import com.intellij.jboss.bpmn.jbpm.providers.ColorProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface RenderBackgroundColor {
  Class<? extends ColorProvider> color();
}
