package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://jbpm.org/4.3/jpdm:taskElemType interface.
 */
public interface Task extends TransitionOwner, JpdlNamedActivity, Graphical, OnOwner {


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
   * Returns the value of the swimlane child.
   *
   * @return the value of the swimlane child.
   */
  @NotNull
  GenericAttributeValue<String> getSwimlane();


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
   * Returns the value of the duedate child.
   *
   * @return the value of the duedate child.
   */
  @NotNull
  GenericAttributeValue<String> getDuedate();


  /**
   * Returns the value of the on-transition child.
   *
   * @return the value of the on-transition child.
   */
  @NotNull
  GenericAttributeValue<OnTransition> getOnTransition();


  /**
   * Returns the value of the completion child.
   *
   * @return the value of the completion child.
   */
  @NotNull
  GenericAttributeValue<String> getCompletion();


  /**
   * Returns the value of the description child.
   *
   * @return the value of the description child.
   */
  @NotNull
  GenericDomValue<String> getDescription();


  /**
   * Returns the value of the assignment-handler child.
   *
   * @return the value of the assignment-handler child.
   */
  @NotNull
  WireObject getAssignmentHandler();


  /**
   * Returns the value of the notification child.
   *
   * @return the value of the notification child.
   */
  @NotNull
  Notification getNotification();


  /**
   * Returns the value of the reminder child.
   *
   * @return the value of the reminder child.
   */
  @NotNull
  Reminder getReminder();


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
}
