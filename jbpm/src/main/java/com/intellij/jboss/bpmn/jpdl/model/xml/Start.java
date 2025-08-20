package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://jbpm.org/4.3/jpdm:startElemType interface.
 */
public interface Start extends TransitionOwner, JpdlNamedActivity, Graphical, OnOwner {


  /**
   * Returns the value of the continue child.
   * <pre>
   * <h3>Attribute null:continue documentation</h3>
   * To specify async continuations.
   *       sync is the default.
   * </pre>
   *
   * @return the value of the continue child.
   */
  @NotNull
  GenericAttributeValue<Continue> getContinue();


  /**
   * Returns the value of the form child.
   * <pre>
   * <h3>Attribute null:form documentation</h3>
   * the resource name of the form in the
   *             deployment.
   * </pre>
   *
   * @return the value of the form child.
   */
  @NotNull
  GenericAttributeValue<String> getForm();


  /**
   * Returns the value of the description child.
   *
   * @return the value of the description child.
   */
  @NotNull
  GenericDomValue<String> getDescription();
}
