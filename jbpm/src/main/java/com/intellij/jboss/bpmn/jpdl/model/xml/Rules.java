package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://jbpm.org/4.3/jpdm:rulesElemType interface.
 */
public interface Rules extends TransitionOwner, JpdlNamedActivity, Graphical, OnOwner {

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
   * Returns the value of the description child.
   *
   * @return the value of the description child.
   */
  @NotNull
  GenericDomValue<String> getDescription();


  /**
   * Returns the list of fact children.
   *
   * @return the list of fact children.
   */
  @NotNull
  List<Fact> getFacts();

  /**
   * Adds new child to the list of fact children.
   *
   * @return created child
   */
  Fact addFact();
}
