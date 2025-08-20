package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.jboss.bpmn.jpdl.model.xml.converters.ProcessDefinitionConverter;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://jbpm.org/4.3/jpdm:sub-processElemType interface.
 */
public interface SubProcess extends TransitionOwner, JpdlNamedActivity, Graphical, OnOwner {

  /**
   * Identifies the sub process by the id.  This means that a specific version of a process definition is referenced
   *
   * @return the value of the sub-process-id child.
   */
  @NotNull
  GenericAttributeValue<String> getSubProcessId();


  /**
   * Returns the value of the sub-process-key child.
   * <pre>
   * <h3>Attribute null:sub-process-key documentation</h3>
   * Identifies the sub process by the key.  This means that the latest
   *               version of the process definition with the given key is referenced.  The latest version
   *               of the process is looked up each time the activity executes.
   * </pre>
   *
   * @return the value of the sub-process-key child.
   */
  @NotNull
  @Convert(ProcessDefinitionConverter.class)
  GenericAttributeValue<ProcessDefinition> getSubProcessKey();

  /**
   * Returns the value of the outcome child.
   * <pre>
   * <h3>Attribute null:outcome documentation</h3>
   * Expression that is evaluated when the sub process
   *             instance ends.  The value is then used for outcome transition mapping.
   * </pre>
   *
   * @return the value of the outcome child.
   */
  @NotNull
  GenericAttributeValue<String> getOutcome();

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
   * Returns the list of description children.
   *
   * @return the list of description children.
   */
  @NotNull
  List<GenericDomValue<String>> getDescriptions();

  /**
   * Returns the list of parameter-in children.
   *
   * @return the list of parameter-in children.
   */
  @NotNull
  List<Parameter> getParameterIns();

  /**
   * Returns the list of parameter-out children.
   *
   * @return the list of parameter-out children.
   */
  @NotNull
  List<Parameter> getParameterOuts();

  /**
   * Returns the list of timer children.
   *
   * @return the list of timer children.
   */
  @NotNull
  List<Timer> getTimers();

  /**
   * Returns the list of swimlane-mapping children.
   *
   * @return the list of swimlane-mapping children.
   */
  @NotNull
  List<SwimlaneMapping> getSwimlaneMappings();
}
