package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ActivitiesContainer extends JpdlDomElement {
  /**
   * Returns the list of start children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:start documentation</h3>
   * Start event
   * </pre>
   *
   * @return the list of start children.
   */
  @NotNull
  List<Start> getStarts();

  /**
   * Adds new child to the list of start children.
   *
   * @return created child
   */
  Start addStart();


  /**
   * Returns the list of end children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:end documentation</h3>
   * End event.
   * </pre>
   *
   * @return the list of end children.
   */
  @NotNull
  List<End> getEnds();

  /**
   * Adds new child to the list of end children.
   *
   * @return created child
   */
  End addEnd();


  /**
   * Returns the list of end-cancel children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:end-cancel documentation</h3>
   * End cancel event.
   * </pre>
   *
   * @return the list of end-cancel children.
   */
  @NotNull
  List<EndCancel> getEndCancels();

  /**
   * Adds new child to the list of end-cancel children.
   *
   * @return created child
   */
  EndCancel addEndCancel();


  /**
   * Returns the list of end-error children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:end-error documentation</h3>
   * End cancel event.
   * </pre>
   *
   * @return the list of end-error children.
   */
  @NotNull
  List<EndError> getEndErrors();

  /**
   * Adds new child to the list of end-error children.
   *
   * @return created child
   */
  EndError addEndError();


  /**
   * Returns the list of state children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:state documentation</h3>
   * A wait state.  When an execution arrives in this
   *         activity, the execution will wait until an external trigger is received
   *         with execution.signal() or execution.getActivityInstance().signal()
   * </pre>
   *
   * @return the list of state children.
   */
  @NotNull
  List<State> getStates();

  /**
   * Adds new child to the list of state children.
   *
   * @return created child
   */
  State addState();


  /**
   * Returns the list of decision children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:decision documentation</h3>
   * Decision gateway: selects one path out of many alternatives.
   *         When an execution comes in, exactly one outgoing transition is taken.
   * </pre>
   *
   * @return the list of decision children.
   */
  @NotNull
  List<Decision> getDecisions();

  /**
   * Adds new child to the list of decision children.
   *
   * @return created child
   */
  Decision addDecision();


  /**
   * Returns the list of fork children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:fork documentation</h3>
   * Spawns multiple concurrent paths of
   *         execution.
   * </pre>
   *
   * @return the list of fork children.
   */
  @NotNull
  List<Fork> getForks();

  /**
   * Adds new child to the list of fork children.
   *
   * @return created child
   */
  Fork addFork();


  /**
   * Returns the list of join children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:join documentation</h3>
   * Spawns multiple concurrent paths of
   *         execution.
   * </pre>
   *
   * @return the list of join children.
   */
  @NotNull
  List<Join> getJoins();

  /**
   * Adds new child to the list of join children.
   *
   * @return created child
   */
  Join addJoin();

  /**
   * Returns the list of jms children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:jms documentation</h3>
   * Sends an email
   * </pre>
   *
   * @return the list of jms children.
   */
  @NotNull
  List<Jms> getJmses();

  /**
   * Adds new child to the list of jms children.
   *
   * @return created child
   */
  Jms addJms();


  /**
   * Returns the list of custom children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:custom documentation</h3>
   * Uses a user defined, custom implementation of
   *         ActivityBehaviour
   * </pre>
   *
   * @return the list of custom children.
   */
  @NotNull
  List<Custom> getCustoms();

  /**
   * Adds new child to the list of custom children.
   *
   * @return created child
   */
  Custom addCustom();


  /**
   * Returns the list of task children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:task documentation</h3>
   * Creates a task in the task component.
   * </pre>
   *
   * @return the list of task children.
   */
  @NotNull
  List<Task> getTasks();

  /**
   * Adds new child to the list of task children.
   *
   * @return created child
   */
  Task addTask();


  /**
   * Returns the list of sub-process children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:sub-process documentation</h3>
   * Waits while a sub process instance is
   *         being executed and continues when the sub process instance ends.
   * </pre>
   *
   * @return the list of sub-process children.
   */
  @NotNull
  List<SubProcess> getSubProcesses();

  /**
   * Adds new child to the list of sub-process children.
   *
   * @return created child
   */
  SubProcess addSubProcess();


  /**
   * Returns the list of group children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:group documentation</h3>
   * Scope enclosing a number of activities.
   * </pre>
   *
   * @return the list of group children.
   */
  @NotNull
  List<Group> getGroups();

  /**
   * Adds new child to the list of group children.
   *
   * @return created child
   */
  Group addGroup();


  /**
   * Returns the list of rules-decision children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:rules-decision documentation</h3>
   * Selects one outgoing transition based on evaluation of rules.
   * </pre>
   *
   * @return the list of rules-decision children.
   */
  @NotNull
  List<RulesDecision> getRulesDecisions();

  /**
   * Adds new child to the list of rules-decision children.
   *
   * @return created child
   */
  RulesDecision addRulesDecision();


  /**
   * Returns the list of rules children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:rules documentation</h3>
   * Evaluates rules after feeding in some facts in a stateful knowledge session.
   * </pre>
   *
   * @return the list of rules children.
   */
  @NotNull
  @SubTagList("rules")
  List<Rules> getRules();

  Rules addRules();
}
