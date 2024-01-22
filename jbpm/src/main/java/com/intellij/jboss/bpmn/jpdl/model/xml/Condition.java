package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://jbpm.org/4.3/jpdm:conditionElemType interface.
 */
public interface Condition extends JpdlDomElement {

  /**
   * Returns the value of the expr child.
   * <pre>
   * <h3>Attribute null:expr documentation</h3>
   * The script text that will be evaluated.
   * </pre>
   *
   * @return the value of the expr child.
   */
  @NotNull
  GenericAttributeValue<String> getExpr();


  /**
   * Returns the value of the lang child.
   * <pre>
   * <h3>Attribute null:lang documentation</h3>
   * Identification of the scripting language
   *                             to use.
   * </pre>
   *
   * @return the value of the lang child.
   */
  @NotNull
  GenericAttributeValue<String> getLang();


  /**
   * Returns the value of the handler child.
   *
   * @return the value of the handler child.
   */
  @NotNull
  WireObject getHandler();
}
