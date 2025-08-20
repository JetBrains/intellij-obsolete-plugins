package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://jbpm.org/4.3/jpdm:decisionElemType interface.
 */
public interface Decision extends TransitionOwner, JpdlNamedActivity, Graphical, OnOwner {

  /**
   * Returns the value of the continue child.
   * <pre>
   * <h3>Attribute null:continue documentation</h3>
   * To specify async continuations. sync is the default.
   * </pre>
   *
   * @return the value of the continue child.
   */
  @NotNull
  GenericAttributeValue<Continue> getContinue();

  /**
   * <pre>
   * <h3>Attribute null:expr documentation</h3>
   * The script that will be evaluated and resolve to the name of the outgoing transition.
   * </pre>
   *
   * @return the value of the expr child.
   */
  @NotNull
  GenericAttributeValue<String> getExpr();


  /**
   * Returns the value of the lang child.
   * <pre>
   * <h3>Attribute null:lang documentation</h3>
   * Identification of the scripting language
   *             to use for the expr attribute.
   * </pre>
   *
   * @return the value of the lang child.
   */
  @NotNull
  GenericAttributeValue<String> getLang();

  /**
   * Returns the value of the description child.
   *
   * @return the value of the description child.
   */
  @NotNull
  GenericDomValue<String> getDescription();


  /**
   * Returns the value of the handler child.
   *
   * @return the value of the handler child.
   */
  @NotNull
  WireObject getHandler();
}
