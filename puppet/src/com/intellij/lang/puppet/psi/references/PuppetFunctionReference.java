package com.intellij.lang.puppet.psi.references;

import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.ide.libraries.PuppetLibraryUtil;
import com.intellij.lang.puppet.ide.navigation.plugins.ruby.PuppetRubyPluginsIndex;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetFunctionsStubsIndex;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.ResolveResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PuppetFunctionReference extends PuppetPolyVariantCachingReferenceWithFullQualifiedName<PsiElement> {


  public PuppetFunctionReference(PsiElement psiElement, TextRange range, String fullQualifiedName) {
    super(psiElement, range, fullQualifiedName);
  }

  @Override
  protected ResolveResult @NotNull [] resolveInner(boolean incompleteCode) {
    final List<PsiElement> elements = new ArrayList<>();
    final List<PsiElement> stubElements = new ArrayList<>();

    PsiElement element = getElement();
    for (PsiElement rubyElement : PuppetRubyPluginsIndex
      .findElementsByKey(PuppetRubyPluginsIndex.SymbolType.FUNCTION, myFullQualifiedName, element.getProject(),
                         element.getResolveScope())) {
      if (PuppetLibraryUtil.isFunctionStubElement(rubyElement)) {
        stubElements.add(rubyElement);
      }
      else {
        elements.add(rubyElement);
      }
    }


    if (elements.isEmpty()) {
      elements.addAll(stubElements);
    }

    elements.addAll(PuppetFunctionsStubsIndex.getInstance().find(myFullQualifiedName, myElement));

    return PsiElementResolveResult.createResults(elements);
  }

  @Override
  public @NotNull String getPresentableName() {
    return PuppetBundle.message("puppet.type.names.function_definition");
  }
}
