package com.intellij.jboss.bpmn.jpdl.graph.nodes;

import com.intellij.jboss.bpmn.jpdl.graph.JpdlEdge;
import com.intellij.jboss.bpmn.jpdl.graph.JpdlNode;
import com.intellij.jboss.bpmn.jpdl.model.xml.Transition;
import org.jetbrains.annotations.NotNull;

public class JpdlBasicEdge implements JpdlEdge {
  private final JpdlNode mySource;
  private final JpdlNode myTarget;
  private final String myName;
  private final Transition myTransition;
  private final boolean myDuplicated;

  public JpdlBasicEdge(final JpdlNode source, final JpdlNode target, final Transition transition, boolean duplicated) {
    mySource = source;
    myTarget = target;
    myName = transition.getName().getStringValue();
    myTransition = transition;
    myDuplicated = duplicated;
  }

  @Override
  public JpdlNode getSource() {
    return mySource;
  }

  @Override
  public JpdlNode getTarget() {
    return myTarget;
  }

  @Override
  public String getName() {
    return myName == null ? "" : myName;
  }

  @Override
  @NotNull
  public Transition getIdentifyingElement() {
    return myTransition;
  }

  @Override
  public boolean isDuplicated() {
    return myDuplicated;
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final JpdlBasicEdge that = (JpdlBasicEdge)o;

    if (mySource != null ? !mySource.equals(that.mySource) : that.mySource != null) return false;
    if (myTarget != null ? !myTarget.equals(that.myTarget) : that.myTarget != null) return false;
    if (myTransition != null ? !myTransition.equals(that.myTransition) : that.myTransition != null) return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = (mySource != null ? mySource.hashCode() : 0);
    result = 31 * result + (myTarget != null ? myTarget.hashCode() : 0);
    result = 31 * result + (myTransition != null ? myTransition.hashCode() : 0);
    return result;
  }
}
