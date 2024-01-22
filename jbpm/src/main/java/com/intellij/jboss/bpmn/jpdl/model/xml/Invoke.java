package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://jbpm.org/4.3/jpdm:invokeElemType interface.
 */
public interface Invoke extends JpdlDomElement {

  /**
   * Returns the value of the method child.
   * <pre>
   * <h3>Attribute null:method documentation</h3>
   * the method name
   * </pre>
   *
   * @return the value of the method child.
   */
  @NotNull
  @Required
  GenericAttributeValue<String> getMethod();


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
