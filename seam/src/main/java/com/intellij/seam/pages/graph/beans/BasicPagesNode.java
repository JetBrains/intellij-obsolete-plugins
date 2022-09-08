package com.intellij.seam.pages.graph.beans;

import com.intellij.icons.AllIcons;
import com.intellij.util.xml.DomElement;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class BasicPagesNode<T extends DomElement> {

  private final T myIdentifyingElement;
  private final @Nls String myName;

  protected BasicPagesNode(@NotNull T identifyingElement, @Nls @Nullable String name) {
    myIdentifyingElement = identifyingElement;
    myName = name;
  }

  public @Nls String getName() {
    return myName;
  }

  @NotNull
  public T getIdentifyingElement() {
    return myIdentifyingElement;
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final BasicPagesNode pagesNode = (BasicPagesNode)o;

    if (!myIdentifyingElement.equals(pagesNode.myIdentifyingElement)) return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = myIdentifyingElement.hashCode();
    result = 31 * result + (myName != null ? myName.hashCode() : 0);
    return result;
  }

  public Icon getIcon() {
    return AllIcons.FileTypes.Any_type;
  }
}
