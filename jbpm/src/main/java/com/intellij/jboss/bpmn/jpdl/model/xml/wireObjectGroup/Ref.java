package com.intellij.jboss.bpmn.jpdl.model.xml.wireObjectGroup;

import com.intellij.jboss.bpmn.jpdl.model.xml.JpdlDomElement;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://jbpm.org/4.3/jpdm:refElemType interface.
 */
public interface Ref extends JpdlDomElement {

  /**
   * Returns the value of the object child.
   * <pre>
   * <h3>Attribute null:object documentation</h3>
   * The name of the referred object
   * </pre>
   *
   * @return the value of the object child.
   */
  @NotNull
  @Required
  GenericAttributeValue<String> getObject();
}
