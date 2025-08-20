package com.intellij.plugins.jboss.arquillian.configuration.container;

public interface ArquillianContainerParameter {
  String getId();

  String getName();

  Class getParameterClass();
}
