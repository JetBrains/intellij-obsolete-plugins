package com.intellij.jboss.bpmn.jpdl.model.xml.wireObjectGroup;

import com.intellij.jboss.bpmn.jpdl.model.xml.JpdlDomElement;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://jbpm.org/4.3/jpdm:propertyElemType interface.
 */
public interface Property extends JpdlDomElement {

  /**
   * Returns the value of the name child.
   *
   * @return the value of the name child.
   */
  @NotNull
  @Required
  GenericAttributeValue<String> getName();

  /**
   * Returns the value of the value child.
   *
   * @return the value of the value child.
   */
  @NotNull
  @Required
  GenericAttributeValue<String> getValue();
}
