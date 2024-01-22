package com.intellij.jboss.bpmn.jbpm.model.render;

import com.intellij.jboss.bpmn.jbpm.providers.ColorProviderImpl;

import java.awt.*;

public class IntermediateEventColorProvider extends ColorProviderImpl {
  public IntermediateEventColorProvider() {
    //noinspection UseJBColor
    super(new Color(190, 143, 79));
  }
}
