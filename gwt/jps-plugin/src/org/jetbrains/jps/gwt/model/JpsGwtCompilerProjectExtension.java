package org.jetbrains.jps.gwt.model;

import org.jetbrains.jps.model.JpsElement;

public interface JpsGwtCompilerProjectExtension extends JpsElement {
  boolean isShowCompilerOutput(JpsGwtModuleExtension extension);
}
