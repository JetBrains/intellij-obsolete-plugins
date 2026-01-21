// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.intentions;

import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public abstract class MutablyNamedIntention extends Intention {
  private @IntentionName String text = null;

  protected abstract @IntentionName String getTextForElement(PsiElement element);

  @Override
  public @NotNull String getText() {
    return text;
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile psiFile) {
    PsiElement element = findMatchingElement(psiFile, editor);
    if (element != null) {
      text = getTextForElement(element);
    }
    return element != null;
  }
}
