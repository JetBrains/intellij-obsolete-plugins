package com.intellij.jboss.bpmn.jbpm.render.pictures;

import com.intellij.jboss.bpmn.jbpm.providers.DefaultIconProvider;
import com.intellij.jboss.bpmn.jbpm.providers.IconProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface RenderIcon {
  String icon() default "";

  Class<? extends IconProvider> iconProvider() default DefaultIconProvider.class;

  VerticalAlignment verticalAlignment() default VerticalAlignment.Top;

  HorizontalAlignment horizontalAlignment() default HorizontalAlignment.Left;

  enum VerticalAlignment {
    Top,
    Center,
    Bottom
  }

  enum HorizontalAlignment {
    Left,
    Center,
    Right
  }
}
