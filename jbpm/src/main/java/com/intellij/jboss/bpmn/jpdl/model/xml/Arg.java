package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://jbpm.org/4.3/jpdm:argType interface.
 * <pre>
 * <h3>Type http://jbpm.org/4.3/jpdm:argType documentation</h3>
 * The method arguments.
 *     Each 'arg' element should have exactly one child element
 *     that represents the value of the argument.
 * </pre>
 */
public interface Arg extends JpdlDomElement {

  /**
   * Returns the value of the type child.
   * <pre>
   * <h3>Attribute null:type documentation</h3>
   * The java class name representing
   *       the type of the method.  This is optional and can be used to
   *       indicate the appropriate method in case of method overloading.
   * </pre>
   *
   * @return the value of the type child.
   */
  @NotNull
  GenericAttributeValue<String> getType();
}
