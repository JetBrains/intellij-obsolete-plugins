package org.jetbrains.jps.dmserver.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.artifact.elements.JpsCompositePackagingElement;
import org.jetbrains.jps.model.module.JpsModuleReference;

/**
 * @author michael.golubev
 */
public interface JpsDMContainerPackagingElement extends JpsCompositePackagingElement {

  String TYPE_ID = "packaging-for-osgi-module";

  @NotNull
  JpsModuleReference getModuleReference();
}
