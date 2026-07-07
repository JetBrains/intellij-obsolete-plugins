package org.jetbrains.plugins.ruby.chef.symbols;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.ruby.chef.ChefUtil;
import org.jetbrains.plugins.ruby.ruby.codeInsight.RubyOverrideImplementUtil;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.Type;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.structure.Symbol;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.structure.SymbolUtil;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.v2.SymbolPsiProcessor;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.v2.TopLevelSymbolImpl;
import org.jetbrains.plugins.ruby.ruby.lang.psi.holders.RContainer;

public class RecipeSymbol extends TopLevelSymbolImpl {
  public RecipeSymbol(@NotNull PsiElement element) {
    super(element);
  }

  @Override
  public boolean processChildren(SymbolPsiProcessor processor, PsiElement invocationPoint) {
    final Symbol chefResourceClassSymbol = SymbolUtil.findConstantByFQN(invocationPoint.getProject(), Type.CLASS, ChefUtil.CHEF_RESOURCE, null);
    if (chefResourceClassSymbol == null) return false;

    final PsiElement classContainer = chefResourceClassSymbol.getPsiElement();
    if (!(classContainer instanceof RContainer)) return false;

    for (RContainer resourceClass : RubyOverrideImplementUtil.getOverridingElements((RContainer)classContainer)) {
      if (!processor.process(new ResourceSymbol(resourceClass, chefResourceClassSymbol))) {
        return false;
      }
    }

    return super.processChildren(processor, invocationPoint);
  }
}
