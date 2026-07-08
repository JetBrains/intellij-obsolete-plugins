package com.intellij.lang.puppet.psi.manipulators;

import com.intellij.lang.puppet.psi.PuppetVariable;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

public class PuppetVariableManipulator extends PuppetLeafOwnerManipulator<PuppetVariable> {

  @Override
  public @NotNull TextRange getRangeInElement(@NotNull PuppetVariable element) {
    PsiElement identifier = element.getNameIdentifier();
    return identifier == null
           ? TextRange.EMPTY_RANGE
           : identifier.getTextRange().shiftRight(-element.getNode().getStartOffset());
  }

  @Override
  protected @NotNull LeafPsiElement getLeafElement(@NotNull PuppetVariable element) {
    PsiElement identifierElement = element.getNameIdentifier();
    if (identifierElement == null) {
      throw new IncorrectOperationException("Variable without name");
    }
    assert identifierElement instanceof LeafPsiElement : "Got " + identifierElement + " instead of LeafPsiElement";
    return (LeafPsiElement)identifierElement;
  }
}
