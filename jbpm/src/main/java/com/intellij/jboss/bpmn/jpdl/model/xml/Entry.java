package com.intellij.jboss.bpmn.jpdl.model.xml;

import org.jetbrains.annotations.NotNull;

/**
 * http://jbpm.org/4.3/jpdm:entryElemType interface.
 */
public interface Entry extends JpdlDomElement {

  /**
   * Returns the value of the key child.
   *
   * @return the value of the key child.
   */
  @NotNull
  Key getKey();


  /**
   * Returns the value of the value child.
   *
   * @return the value of the value child.
   */
  @NotNull
  Value getValue();
}
