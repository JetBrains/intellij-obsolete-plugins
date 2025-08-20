package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://jbpm.org/4.3/jpdm:end-errorElemType interface.
 */
public interface EndError extends JpdlNamedActivity, Graphical, OnOwner {


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
   * Returns the value of the ends child.
   *
   * @return the value of the ends child.
   */
  @NotNull
  GenericAttributeValue<Ends> getEnds();


  /**
   * Returns the value of the description child.
   *
   * @return the value of the description child.
   */
  @NotNull
  GenericDomValue<String> getDescription();
}
