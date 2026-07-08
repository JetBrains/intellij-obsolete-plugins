package com.intellij.lang.puppet.psi.references;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public abstract class PuppetPolyVariantCachingReferenceWithFullQualifiedName<T extends PsiElement>
  extends PuppetPolyVariantCachingReferenceBase<T> {
  protected final @NonNls String myFullQualifiedName;

  public PuppetPolyVariantCachingReferenceWithFullQualifiedName(T psiElement, String fullQualifiedName) {
    this(psiElement, null, fullQualifiedName);
  }

  public PuppetPolyVariantCachingReferenceWithFullQualifiedName(T psiElement, TextRange range, String fullQualifiedName) {
    super(psiElement, range);
    myFullQualifiedName = fullQualifiedName;
  }

  protected @NotNull String adjustNewName(@NonNls String newElementName) {
    newElementName = StringUtil.toLowerCase(newElementName);
    String originalText = getRangeInElement().substring(getElement().getText());
    return StringUtil.isCapitalized(originalText) ? StringUtil.capitalize(newElementName) : newElementName;
  }

  @Override
  public PsiElement handleElementRename(@NotNull @NonNls String newElementName) throws IncorrectOperationException {
    return super.handleElementRename(adjustNewName(newElementName));
  }
}
