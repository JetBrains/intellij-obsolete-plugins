package com.intellij.jboss.bpmn.jbpm.model.render;

import com.intellij.jboss.bpmn.jbpm.providers.ColorProviderImpl;

import java.awt.*;

public class EndEventColorProvider extends ColorProviderImpl {
  public EndEventColorProvider() {
    //noinspection UseJBColor
    super(new Color(198, 91, 77));
  }
}
