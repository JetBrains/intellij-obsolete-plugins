package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.NameValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://jbpm.org/4.3/jpdm:processElemType interface.
 */
public interface ProcessDefinition extends ActivitiesContainer, OnOwner, InvokersOwner, JpdlDomElement {

  /**
   * Returns the value of the name child.
   * <pre>
   * <h3>Attribute null:name documentation</h3>
   * The process name. Multiple processes can be deployed with the same name, as long as they have a different version.
   * </pre>
   *
   * @return the value of the name child.
   */
  @NotNull
  @Required
  @NameValue
  GenericAttributeValue<String> getName();

  /**
   * Returns the value of the key child.
   * <pre>
   * <h3>Attribute null:key documentation</h3>
   * The key can be used to provide a short acronym that will replace the name as the basis for the generated
   *             process definition id
   * </pre>
   *
   * @return the value of the key child.
   */
  @NotNull
  GenericAttributeValue<String> getKey();


  /**
   * Returns the value of the version child.
   * <pre>
   * <h3>Attribute null:version documentation</h3>
   * Indicates the sequence number of this version for all processes with the same name. By specifying a version
   *             automatic deployment can figure out if this process is already deployed or not.
   * </pre>
   *
   * @return the value of the version child.
   */
  @NotNull
  GenericAttributeValue<Integer> getVersion();


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
   * Returns the list of swimlane children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:swimlane documentation</h3>
   * A process role.
   * </pre>
   *
   * @return the list of swimlane children.
   */
  @NotNull
  List<Swimlane> getSwimlanes();

  /**
   * Adds new child to the list of swimlane children.
   *
   * @return created child
   */
  Swimlane addSwimlane();


  /**
   * Returns the list of timer children.
   *
   * @return the list of timer children.
   */
  @NotNull
  List<Timer> getTimers();

  /**
   * Adds new child to the list of timer children.
   *
   * @return created child
   */
  Timer addTimer();


  /**
   * Returns the list of migrate-instances children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:migrate-instances documentation</h3>
   * Information to migrate instances of previously deployed process definitions
   *     to the new one
   * </pre>
   *
   * @return the list of migrate-instances children.
   */
  @NotNull
  List<MigrateInstances> getMigrateInstanceses();

  /**
   * Adds new child to the list of migrate-instances children.
   *
   * @return created child
   */
  MigrateInstances addMigrateInstances();
}
