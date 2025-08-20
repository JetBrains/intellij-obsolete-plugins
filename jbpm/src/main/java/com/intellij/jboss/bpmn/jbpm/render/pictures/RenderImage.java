package com.intellij.jboss.bpmn.jbpm.render.pictures;

import com.intellij.jboss.bpmn.jbpm.providers.DefaultImageProvider;
import com.intellij.jboss.bpmn.jbpm.providers.ImageProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface RenderImage {
  /**
   * @return Path to image resource ({@code /foo/bar/MyIcon.png}) or FQN (w/o "icons" package) to icon field ({@code MyIcons.CustomIcon}).
   */
  String icon() default "";

  Class<? extends ImageProvider> imageProvider() default DefaultImageProvider.class;
}
