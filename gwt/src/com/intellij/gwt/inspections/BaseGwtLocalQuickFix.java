package com.intellij.gwt.inspections;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import org.jetbrains.annotations.NotNull;

public abstract class BaseGwtLocalQuickFix implements LocalQuickFix {
  private final @IntentionName String myName;

  protected BaseGwtLocalQuickFix(@IntentionName String name) {
    myName = name;
  }

  @Override
  public @NotNull String getName() {
    return myName;
  }

  @Override
  public abstract @IntentionFamilyName @NotNull String getFamilyName();
}
