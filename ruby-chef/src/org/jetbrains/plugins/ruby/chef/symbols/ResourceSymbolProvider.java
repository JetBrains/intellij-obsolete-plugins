package org.jetbrains.plugins.ruby.chef.symbols;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.ruby.chef.ChefUtil;
import org.jetbrains.plugins.ruby.chef.codeInsight.completion.ChefResourceReferenceContributor;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.RubySymbolProvider;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.Type;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.structure.Symbol;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.structure.SymbolUtil;
import org.jetbrains.plugins.ruby.ruby.codeInsight.types.ContextImpl;
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.classes.RClass;
import org.jetbrains.plugins.ruby.ruby.lang.psi.holders.RTopLevelContainer;
import org.jetbrains.plugins.ruby.ruby.lang.psi.iterators.RCodeBlock;
import org.jetbrains.plugins.ruby.ruby.lang.psi.iterators.RDoBlockCall;
import org.jetbrains.plugins.ruby.utils.NamingConventions;


public final class ResourceSymbolProvider implements RubySymbolProvider {
  @Override
  public ContextImpl getAccessContext(@NotNull PsiElement element) {
    if (!ChefUtil.isInCookbook(element)) return null;

    if (SymbolUtil.getScopeContextWithCaching(element) instanceof RecipeSymbol) return ContextImpl.INSTANCE_PRIVATE;
    return null;
  }

  @Override
  public Symbol getSymbolScope(@NotNull PsiElement element) {
    if (!ChefUtil.isInCookbook(element)) return null;

    final Symbol symbol = findParentResourceSymbol(element);
    if (symbol != null) return symbol;

    if (!ChefResourceReferenceContributor.Holder.CHEF_RESOURCE_NAME_COMPLETION.accepts(element)) return null;

    RTopLevelContainer container = PsiTreeUtil.getParentOfType(element, RTopLevelContainer.class);
    if (container != null) {
      return new RecipeSymbol(container);
    }

    return null;
  }

  public static @Nullable RClass findParentResourceClass(final @Nullable PsiElement element) {
    if (!ChefUtil.isInCookbook(element)) return null;
    if (!ChefResourceReferenceContributor.Holder.CHEF_RESOURCE_CONTENT_PATTERN.accepts(element)) return null;

    final RDoBlockCall doBlockCall = PsiTreeUtil.getParentOfType(element, RDoBlockCall.class, true);
    final RCodeBlock codeBlock = PsiTreeUtil.getParentOfType(element, RCodeBlock.class, true);

    if (doBlockCall != null && codeBlock != null) {
      String text = doBlockCall.getCall().getCommand();
      if (text == null) return null;

      String resourceName = ChefUtil.CHEF_RESOURCE + "::" + NamingConventions.toCamelCase(text);
      final Symbol resourceSymbol = SymbolUtil.findConstantByFQN(element.getProject(), Type.CLASS, resourceName, null);
      if (resourceSymbol == null) return null;

      final PsiElement resourceClassElement = resourceSymbol.getPsiElement();
      return !(resourceClassElement instanceof RClass) ? null : (RClass)resourceClassElement;
    }

    return null;
  }

  public static @Nullable Symbol findParentResourceSymbol(final @Nullable PsiElement element) {
    final RClass container = findParentResourceClass(element);

    return container != null ? new ResourceSymbol(container, null) : null;
  }
}
