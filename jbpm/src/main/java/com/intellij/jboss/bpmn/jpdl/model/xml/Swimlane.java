package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://jbpm.org/4.3/jpdm:swimlaneElemType interface.
 */
public interface Swimlane extends JpdlDomElement {

  /**
   * Returns the value of the name child.
   *
   * @return the value of the name child.
   */
  @NotNull
  @Required
  GenericAttributeValue<String> getName();

  /**
   * Returns the value of the assignee child.
   * <pre>
   * <h3>Attribute null:assignee documentation</h3>
   * Expression that resolves to a userId referencing
   *       the person to which the task or swimlane will be assigned.
   * </pre>
   *
   * @return the value of the assignee child.
   */
  @NotNull
  GenericAttributeValue<String> getAssignee();


  /**
   * Returns the value of the assignee-lang child.
   * <pre>
   * <h3>Attribute null:assignee-lang documentation</h3>
   * Expression language for the assignee attribute.
   * </pre>
   *
   * @return the value of the assignee-lang child.
   */
  @NotNull
  GenericAttributeValue<String> getAssigneeLang();


  /**
   * Returns the value of the candidate-users child.
   * <pre>
   * <h3>Attribute null:candidate-users documentation</h3>
   * Expression that resolves to a comma separated
   *       list of userId's. All the referred people will be candidates for
   *       take the task or swimlane.
   * </pre>
   *
   * @return the value of the candidate-users child.
   */
  @NotNull
  GenericAttributeValue<String> getCandidateUsers();


  /**
   * Returns the value of the candidate-users-lang child.
   * <pre>
   * <h3>Attribute null:candidate-users-lang documentation</h3>
   * Expression language for the
   *       candidate-users attribute.
   * </pre>
   *
   * @return the value of the candidate-users-lang child.
   */
  @NotNull
  GenericAttributeValue<String> getCandidateUsersLang();


  /**
   * Returns the value of the candidate-groups child.
   * <pre>
   * <h3>Attribute null:candidate-groups documentation</h3>
   * Resolves to a comma separated list of groupId's.
   *       All the referred people will be candidates to
   *       take the task or swimlane.
   * </pre>
   *
   * @return the value of the candidate-groups child.
   */
  @NotNull
  GenericAttributeValue<String> getCandidateGroups();


  /**
   * Returns the value of the candidate-groups-lang child.
   * <pre>
   * <h3>Attribute null:candidate-groups-lang documentation</h3>
   * Expression language for the
   *       candidate-groups attribute.
   * </pre>
   *
   * @return the value of the candidate-groups-lang child.
   */
  @NotNull
  GenericAttributeValue<String> getCandidateGroupsLang();


  /**
   * Returns the value of the description child.
   *
   * @return the value of the description child.
   */
  @NotNull
  GenericDomValue<String> getDescription();
}
