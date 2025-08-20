package com.intellij.jboss.bpmn.jpdl.model.xml;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://jbpm.org/4.3/jpdm:constructorElemType interface.
 */
public interface Constructor extends JpdlDomElement {

  /**
   * Returns the list of arg children.
   *
   * @return the list of arg children.
   */
  @NotNull
  List<Arg> getArgs();

  /**
   * Adds new child to the list of arg children.
   *
   * @return created child
   */
  Arg addArg();
}
