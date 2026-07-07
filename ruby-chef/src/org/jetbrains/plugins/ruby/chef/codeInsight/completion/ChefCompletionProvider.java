package org.jetbrains.plugins.ruby.chef.codeInsight.completion;

import org.jetbrains.plugins.ruby.chef.symbols.ResourceSymbolProvider;
import org.jetbrains.plugins.ruby.ruby.codeInsight.completion.RubyCompletionProvider;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.structure.Symbol;
import org.jetbrains.plugins.ruby.ruby.lang.psi.RPsiElement;

public final class ChefCompletionProvider extends RubyCompletionProvider {
  @Override
  protected boolean acceptReswords(final RPsiElement element) {
    final Symbol symbol = ResourceSymbolProvider.findParentResourceSymbol(element);
    return symbol == null && super.acceptReswords(element);
  }
}
