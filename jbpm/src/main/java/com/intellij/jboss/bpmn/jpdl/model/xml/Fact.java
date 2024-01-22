package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://jbpm.org/4.3/jpdm:factElemType interface.
 */
public interface Fact extends JpdlDomElement {

  /**
   * Returns the value of the var child.
   *
   * @return the value of the var child.
   */
  @NotNull
  GenericAttributeValue<String> getVar();


  /**
   * Returns the value of the expr child.
   *
   * @return the value of the expr child.
   */
  @NotNull
  GenericAttributeValue<String> getExpr();
}
