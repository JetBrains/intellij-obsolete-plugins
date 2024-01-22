package com.intellij.jboss.bpmn.jpdl.model.xml.wireObjectGroup;

import com.intellij.jboss.bpmn.jpdl.model.xml.JpdlDomElement;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://jbpm.org/4.3/jpdm:jndiElemType interface.
 */
public interface Jndi extends JpdlDomElement {

  /**
   * Returns the value of the jndi-name child.
   *
   * @return the value of the jndi-name child.
   */
  @NotNull
  @Required
  GenericAttributeValue<String> getJndiName();
}
