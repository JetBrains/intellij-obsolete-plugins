package com.intellij.jboss.bpmn.jpdl.model.xml;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface TransitionOwner extends JpdlDomElement {

  /**
   * Returns the list of transition children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:transition documentation</h3>
   * A transition from one activity to another.
   * </pre>
   *
   * @return the list of transition children.
   */
  @NotNull
  List<Transition> getTransitions();

  /**
   * Adds new child to the list of transition children.
   *
   * @return created child
   */
  Transition addTransition();
}
