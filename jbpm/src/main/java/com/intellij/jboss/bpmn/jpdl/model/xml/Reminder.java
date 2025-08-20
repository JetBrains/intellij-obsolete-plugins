package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://jbpm.org/4.3/jpdm:reminderElemType interface.
 */
public interface Reminder extends JpdlDomElement {

  /**
   * Returns the value of the duedate child.
   *
   * @return the value of the duedate child.
   */
  @NotNull
  GenericAttributeValue<String> getDuedate();


  /**
   * Returns the value of the repeat child.
   *
   * @return the value of the repeat child.
   */
  @NotNull
  GenericAttributeValue<String> getRepeat();


  /**
   * Returns the value of the continue child.
   *
   * @return the value of the continue child.
   */
  @NotNull
  GenericAttributeValue<Continue> getContinue();


  /**
   * Returns the value of the template child.
   *
   * @return the value of the template child.
   */
  @NotNull
  GenericAttributeValue<String> getTemplate();
}
