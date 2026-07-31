package com.intellij.gwt.inspections;

import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public abstract class BaseGwtLocalQuickFixOnPsiElement extends LocalQuickFixOnPsiElement {
  private final @IntentionName String myName;
  private final @IntentionFamilyName String myFamilyName;

  protected BaseGwtLocalQuickFixOnPsiElement(@NotNull @IntentionFamilyName String familyName, @NotNull @IntentionName String name, @NotNull PsiElement element) {
    this(familyName, name, element, element);
  }

  protected BaseGwtLocalQuickFixOnPsiElement(@NotNull @IntentionFamilyName String familyName, @NotNull @IntentionName String name,
                                             @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
    super(startElement, endElement);
    myName = name;
    myFamilyName = familyName;
  }

  @Override
  public final @NotNull String getText() {
    return myName;
  }

  @Override
  public final @Nls @NotNull String getFamilyName() {
    return myFamilyName;
  }
}
