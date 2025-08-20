package com.intellij.jboss.bpmn.jpdl.graph.nodes;

import com.intellij.jboss.bpmn.jpdl.graph.JpdlNode;
import com.intellij.jboss.bpmn.jpdl.model.xml.JpdlNamedActivity;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class JpdlBasicNode<T extends JpdlNamedActivity> implements JpdlNode<T> {

  private final T myIdentifyingElement;
  private final @Nls String myName;

  protected JpdlBasicNode(@NotNull final T identifyingElement) {
    this(identifyingElement, getNodeName(identifyingElement));
  }

  protected JpdlBasicNode(@NotNull final T identifyingElement, @Nls @Nullable String name) {
    myIdentifyingElement = identifyingElement;
    myName = name == null ? "" : name;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  @NotNull
  public T getIdentifyingElement() {
    return myIdentifyingElement;
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final JpdlBasicNode that = (JpdlBasicNode)o;

    if (!myIdentifyingElement.equals(that.myIdentifyingElement)) return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = myIdentifyingElement.hashCode();
    result = 31 * result + (myName != null ? myName.hashCode() : 0);
    return result;
  }

  private static @Nullable @NlsSafe String getNodeName(@NotNull JpdlNamedActivity identifyingElement) {
    return identifyingElement.getName().getStringValue();
  }
}
