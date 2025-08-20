package com.intellij.jboss.bpmn.jbpm.model.render;

import com.intellij.jboss.bpmn.jbpm.providers.ColorProviderImpl;

import java.awt.*;

public class StartEventColorProvider extends ColorProviderImpl {
  public StartEventColorProvider() {
    //noinspection UseJBColor
    super(new Color(48, 165, 78));
  }
}