package com.intellij.seam.pageflow.graph.impl;

import com.intellij.seam.pageflow.graph.PageflowNode;
import com.intellij.util.xml.DomElement;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PageflowBasicNode<T extends DomElement> implements PageflowNode<T> {

  private final T myIdentifyingElement;
  private final @Nls String myName;

  protected PageflowBasicNode(@NotNull final T identifyingElement, @Nullable @Nls String name) {
    myIdentifyingElement = identifyingElement;
    myName = name;
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

    final PageflowBasicNode that = (PageflowBasicNode)o;

    if (!myIdentifyingElement.equals(that.myIdentifyingElement)) return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = myIdentifyingElement.hashCode();
    result = 31 * result + (myName != null ? myName.hashCode() : 0);
    return result;
  }
}
