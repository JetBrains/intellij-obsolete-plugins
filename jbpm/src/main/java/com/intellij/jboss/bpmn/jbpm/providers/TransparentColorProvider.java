package com.intellij.jboss.bpmn.jbpm.providers;

import java.awt.*;

public class TransparentColorProvider extends ColorProviderImpl {
  protected TransparentColorProvider() {
    //noinspection UseJBColor
    super(new Color(1, 1, 1, 1));
  }
}
