package com.intellij.seam.utils.beans;

import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;

public class ContextVariable {
  private final CommonModelElement myModelElement;
  private final String myName;
  private final PsiType myType;

  public ContextVariable(@NotNull CommonModelElement modelElement, @NotNull final String name, @NotNull final PsiType type) {
    myModelElement = modelElement;
    myName = name;
    myType = type;
  }


  @NotNull
  public CommonModelElement getModelElement() {
    return myModelElement;
  }

  @NotNull
  public String getName() {
    return myName;
  }

  @NotNull
  public PsiType getType() {
    return myType;
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final ContextVariable that = (ContextVariable)o;

    if (!myModelElement.equals(that.myModelElement)) return false;
    if (!myName.equals(that.myName)) return false;
    if (!myType.equals(that.myType)) return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = myModelElement.hashCode();
    result = 31 * result + myName.hashCode();
    result = 31 * result + myType.hashCode();
    return result;
  }
}
