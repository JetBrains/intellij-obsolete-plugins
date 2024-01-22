package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://jbpm.org/4.3/jpdm:joinElemType interface.
 */
public interface Join extends TransitionOwner, JpdlNamedActivity, Graphical, OnOwner {

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
   * Returns the value of the multiplicity child.
   *
   * @return the value of the multiplicity child.
   */
  @NotNull
  GenericAttributeValue<Integer> getMultiplicity();


  /**
   * Returns the value of the lockmode child.
   *
   * @return the value of the lockmode child.
   */
  @NotNull
  GenericAttributeValue<Lockmode> getLockmode();


  /**
   * Returns the value of the description child.
   *
   * @return the value of the description child.
   */
  @NotNull
  GenericDomValue<String> getDescription();
}
