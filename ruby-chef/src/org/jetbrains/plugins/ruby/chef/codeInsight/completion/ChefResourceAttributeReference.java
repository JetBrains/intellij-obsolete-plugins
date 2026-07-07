package org.jetbrains.plugins.ruby.chef.codeInsight.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.ruby.ruby.codeInsight.references.RNamedReference;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.Types;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.structure.Symbol;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.structure.SymbolUtil;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.v2.ClassModuleSymbol;
import org.jetbrains.plugins.ruby.ruby.codeInsight.types.ContextImpl;
import org.jetbrains.plugins.ruby.ruby.codeInsight.types.RType;
import org.jetbrains.plugins.ruby.ruby.codeInsight.types.RTypeFactory;
import org.jetbrains.plugins.ruby.ruby.lang.psi.RPsiElement;

import java.util.function.Predicate;

import static org.jetbrains.plugins.ruby.chef.ChefUtil.RESOURCE;
import static org.jetbrains.plugins.ruby.ruby.codeInsight.completion.ReferenceCompletionUtil.addLookupItemsByType;

class ChefResourceAttributeReference extends RNamedReference {
  ChefResourceAttributeReference(@NotNull RPsiElement element,
                                        @NotNull RPsiElement realContext) {
    super(element, realContext);
  }

  @Override
  protected boolean isConstantReference() {
    return false;
  }

  @Override
  protected @NotNull Predicate<Symbol> createFilter() {
    return Symbol.Filter.byTypes(Types.METHODS).and(symbol -> {
      final Symbol parentSymbol = symbol.getParentSymbol();
      if (parentSymbol instanceof ClassModuleSymbol) {
        return ((ClassModuleSymbol)parentSymbol).getFQN().getFullPath().contains(RESOURCE);
      }

      return false;
    });
  }

  @Override
  protected void addCompletionVariants(@NotNull Predicate<Symbol> filter,
                                       ContextImpl context,
                                       @NotNull Consumer<LookupElement> elementConsumer) {
    var realContext = getRealContext();
    if (realContext == null) {
      return;
    }
    final Symbol contextSymbol = SymbolUtil.getScopeContextWithCaching(realContext);

    if (contextSymbol == null) {
      return;
    }
    final RType type = RTypeFactory.createTypeBySymbol(contextSymbol);
    addLookupItemsByType(type, filter, null, false, realContext, elementConsumer);
  }
}
