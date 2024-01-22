package com.intellij.jboss.bpmn.jbpm.render.label;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface RenderLabelPosition {
  // For values please refer to http://docs.yworks.com/yfiles/doc/api/y/view/NodeLabel.html

  byte modelSpecifier();

  byte positionSpecifier();
}
