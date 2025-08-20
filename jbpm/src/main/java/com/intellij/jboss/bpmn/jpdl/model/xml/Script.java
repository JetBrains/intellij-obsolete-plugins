package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.Attribute;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://jbpm.org/4.3/jpdm:scriptElemType interface.
 */
public interface Script extends TransitionOwner, JpdlNamedActivity, Graphical, OnOwner {


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
   * Returns the value of the expr child.
   * <pre>
   * <h3>Attribute null:expr documentation</h3>
   * The script text that will be evaluated.  This
   *       is mutually exclusive with the expression element.
   * </pre>
   *
   * @return the value of the expr child.
   */
  @NotNull
  @Attribute("expr")
  GenericAttributeValue<String> getExprAttr();


  /**
   * Returns the value of the lang child.
   * <pre>
   * <h3>Attribute null:lang documentation</h3>
   * Identification of the scripting language
   *       to use.
   * </pre>
   *
   * @return the value of the lang child.
   */
  @NotNull
  @Attribute("lang")
  GenericAttributeValue<String> getLangAttr();


  /**
   * Returns the value of the var child.
   * <pre>
   * <h3>Attribute null:var documentation</h3>
   * Name of the variable in which the result
   *       of the script evaluation will be stored
   * </pre>
   *
   * @return the value of the var child.
   */
  @NotNull
  @Attribute("var")
  GenericAttributeValue<String> getVarAttr();


  /**
   * Returns the list of description children.
   *
   * @return the list of description children.
   */
  @NotNull
  List<GenericDomValue<String>> getDescriptions();

  /**
   * Adds new child to the list of description children.
   *
   * @return created child
   */
  GenericDomValue<String> addDescription();


  /**
   * Returns the value of the text child.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:text documentation</h3>
   * The content of this expression element
   *         is the script text that will be evaluated.  This is mutually
   *         exclusive with the expression attribute.
   * </pre>
   *
   * @return the value of the text child.
   */
  @NotNull
  GenericDomValue<String> getText();


  /**
   * Returns the value of the expr child.
   * <pre>
   * <h3>Attribute null:expr documentation</h3>
   * The script text that will be evaluated.  This
   *       is mutually exclusive with the expression element.
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
   *       to use.
   * </pre>
   *
   * @return the value of the lang child.
   */
  @NotNull
  GenericAttributeValue<String> getLang();


  /**
   * Returns the value of the var child.
   * <pre>
   * <h3>Attribute null:var documentation</h3>
   * Name of the variable in which the result
   *       of the script evaluation will be stored
   * </pre>
   *
   * @return the value of the var child.
   */
  @NotNull
  GenericAttributeValue<String> getVar();
}
