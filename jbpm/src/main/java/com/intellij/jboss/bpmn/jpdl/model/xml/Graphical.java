package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

public interface Graphical extends JpdlDomElement {

  /**
   * Returns the value of the g child.
   * <pre>
   * <h3>Attribute null:g documentation</h3>
   * Graphical information used by process designer tool.
   * </pre>
   *
   * @return the value of the g child.
   */
  @NotNull
  GenericAttributeValue<String> getG();
}
