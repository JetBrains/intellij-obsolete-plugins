package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://jbpm.org/4.3/jpdm:activity-mappingElemType interface.
 */
public interface ActivityMapping extends JpdlDomElement {

  /**
   * Returns the value of the old-name child.
   * <pre>
   * <h3>Attribute null:old-name documentation</h3>
   * The name of the activity in the previously deployed process definition.
   * </pre>
   *
   * @return the value of the old-name child.
   */
  @NotNull
  @Required
  GenericAttributeValue<String> getOldName();


  /**
   * Returns the value of the new-name child.
   * <pre>
   * <h3>Attribute null:new-name documentation</h3>
   * The name of the activity in the newly deployed process definition
   * </pre>
   *
   * @return the value of the new-name child.
   */
  @NotNull
  @Required
  GenericAttributeValue<String> getNewName();
}
