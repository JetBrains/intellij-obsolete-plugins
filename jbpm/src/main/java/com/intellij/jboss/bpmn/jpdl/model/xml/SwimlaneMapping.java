package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://jbpm.org/4.3/jpdm:swimlane-mappingElemType interface.
 */
public interface SwimlaneMapping extends JpdlDomElement {

  /**
   * Returns the value of the swimlane child.
   *
   * @return the value of the swimlane child.
   */
  @NotNull
  @Required
  GenericAttributeValue<String> getSwimlane();


  /**
   * Returns the value of the sub-swimlane child.
   *
   * @return the value of the sub-swimlane child.
   */
  @NotNull
  @Required
  GenericAttributeValue<String> getSubSwimlane();
}
