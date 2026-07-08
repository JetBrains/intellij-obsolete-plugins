package com.intellij.lang.puppet.ide.liveTemplates;

import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.psi.PuppetPsiFileImpl;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class PuppetLiveTemplateContextType extends TemplateContextType {
  public PuppetLiveTemplateContextType() {
    super(PuppetBundle.message("puppet.templates.context.file"));
  }

  @Override
  public boolean isInContext(@NotNull PsiFile file, int offset) {
    return file instanceof PuppetPsiFileImpl;
  }
}
