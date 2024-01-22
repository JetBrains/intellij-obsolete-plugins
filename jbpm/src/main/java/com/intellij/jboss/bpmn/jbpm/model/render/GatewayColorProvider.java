package com.intellij.jboss.bpmn.jbpm.model.render;

import com.intellij.jboss.bpmn.jbpm.providers.ColorProviderImpl;

import java.awt.*;

public class GatewayColorProvider extends ColorProviderImpl {
  public GatewayColorProvider() {
    //noinspection UseJBColor
    super(new Color(134, 101, 156));
  }
}