package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://jbpm.org/4.3/jpdm:qlType interface.
 */
public interface Ql extends JpdlDomElement {

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


  /**
   * Returns the value of the unique child.
   * <pre>
   * <h3>Attribute null:unique documentation</h3>
   * Does this query return a unique result or a list
   * </pre>
   *
   * @return the value of the unique child.
   */
  @NotNull
  GenericAttributeValue<String> getUnique();


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
   * Returns the value of the query child.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:query documentation</h3>
   * The query text.
   * </pre>
   *
   * @return the value of the query child.
   */
  @NotNull
  @Required
  GenericDomValue<String> getQuery();


  /**
   * Returns the value of the parameters child.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:parameters documentation</h3>
   * Query parameters.
   * </pre>
   *
   * @return the value of the parameters child.
   */
  @NotNull
  Parameters getParameters();
}
