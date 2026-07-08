package com.intellij.lang.puppet.psi.manipulators;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

public class PuppetLeafOwnerManipulator<T extends PsiElement> extends AbstractElementManipulator<T> {
  protected @NotNull LeafPsiElement getLeafElement(@NotNull T element) {
    PsiElement child = element.getFirstChild();
    assert child instanceof LeafPsiElement : "First child of " + element + " is " + child + " instead of LeafPsielement";
    return (LeafPsiElement)child;
  }

  @Override
  public T handleContentChange(@NotNull T element, @NotNull TextRange range, String newContent)
    throws IncorrectOperationException {
    LeafPsiElement leafElement = getLeafElement(element);

    int offsetInParent = leafElement.getStartOffsetInParent();
    String oldText = leafElement.getText();
    TextRange nameRange = range.shiftRight(-offsetInParent);
    leafElement.replaceWithText(nameRange.replace(oldText, newContent));

    return element;
  }
}
