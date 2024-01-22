package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://jbpm.org/4.3/jpdm:parameterType interface.
 */
public interface Parameter extends JpdlDomElement {

  /**
   * Returns the value of the subvar child.
   * <pre>
   * <h3>Attribute null:subvar documentation</h3>
   * The name of the sub process variable.
   * </pre>
   *
   * @return the value of the subvar child.
   */
  @NotNull
  GenericAttributeValue<String> getSubvar();


  /**
   * Returns the value of the expr child.
   * <pre>
   * <h3>Attribute null:expr documentation</h3>
   * An expression for which the resulting
   *       value will be used as value.
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
   * Language of the expression.
   * </pre>
   *
   * @return the value of the lang child.
   */
  @NotNull
  GenericAttributeValue<String> getLang();


  /**
   * Returns the value of the var child.
   * <pre>
   * <h3>Attribute null:var documentation</h3>
   * Name of the process variable
   *       in the super process execution..
   * </pre>
   *
   * @return the value of the var child.
   */
  @NotNull
  GenericAttributeValue<String> getVar();
}
