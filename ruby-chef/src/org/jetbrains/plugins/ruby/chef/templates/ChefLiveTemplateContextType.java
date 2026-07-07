package org.jetbrains.plugins.ruby.chef.templates;

import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.ruby.chef.ChefUtil;
import org.jetbrains.plugins.ruby.ruby.templates.RubyLiveTemplateContextType;

public final class ChefLiveTemplateContextType extends RubyLiveTemplateContextType.Generic {
  @Override
  public boolean isInContext(@NotNull PsiFile file, int offset) {
    return super.isInContext(file, offset) && ChefUtil.isInCookbook(file);
  }
}