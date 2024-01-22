package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://jbpm.org/4.3/jpdm:timerElemType interface.
 */
public interface Timer extends EventListenerOwner, InvokersOwner, JpdlDomElement {

  /**
   * Returns the value of the duedate child.
   * <pre>
   * <h3>Attribute null:duedate documentation</h3>
   * Timer duedate expression that defines the duedate of this
   *         timer relative to the creation time of the timer.  E.g. '2 hours' or '4 business days'
   * </pre>
   *
   * @return the value of the duedate child.
   */
  @NotNull
  GenericAttributeValue<String> getDuedate();


  /**
   * Returns the value of the repeat child.
   * <pre>
   * <h3>Attribute null:repeat documentation</h3>
   * Timer duedate expression that defines repeated scheduling
   *         relative to the last timer fire event.  E.g. '2 hours' or '4 business days'
   * </pre>
   *
   * @return the value of the repeat child.
   */
  @NotNull
  GenericAttributeValue<String> getRepeat();


  /**
   * Returns the value of the duedatetime child.
   * <pre>
   * <h3>Attribute null:duedatetime documentation</h3>
   * Absolute time in format {@code HH:mm dd/MM/yyyy}
   *         (see SimpleDateFormat).  The format for the absolute time can be customized in the
   *         jbpm configuration.
   * </pre>
   *
   * @return the value of the duedatetime child.
   */
  @NotNull
  GenericAttributeValue<String> getDuedatetime();


  /**
   * Returns the value of the description child.
   *
   * @return the value of the description child.
   */
  @NotNull
  GenericDomValue<String> getDescription();
}
