package com.intellij.jboss.bpmn.jpdl.model.xml.wireObjectGroup;

import com.intellij.jboss.bpmn.jpdl.model.xml.JpdlDomElement;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://jbpm.org/4.3/jpdm:intElemType interface.
 */
public interface Int extends JpdlDomElement {

  /**
   * Returns the value of the name child.
   * <pre>
   * <h3>Attribute null:name documentation</h3>
   * The name of the object.  It's optional and serves
   *             as an id to refer to this object from other object declarations.  This name can
   *             also be used lookup the object programmatically.
   * </pre>
   *
   * @return the value of the name child.
   */
  @NotNull
  GenericAttributeValue<String> getName();


  /**
   * Returns the value of the value child.
   *
   * @return the value of the value child.
   */
  @NotNull
  @Required
  GenericAttributeValue<Integer> getValue();
}
