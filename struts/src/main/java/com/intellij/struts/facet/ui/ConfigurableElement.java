package com.intellij.struts.facet.ui;

import com.intellij.openapi.options.ConfigurationException;

/**
 * @author Dmitry Avdeev
 */
public interface ConfigurableElement {
  boolean isModified();

  void apply();

  void reset();
}
