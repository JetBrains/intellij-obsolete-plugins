package com.intellij.lang.puppet.psi.references;

import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.ide.navigation.plugins.ruby.PuppetRubyPluginsIndex;
import com.intellij.lang.puppet.psi.PuppetDataTypesManager;
import com.intellij.lang.puppet.psi.PuppetPsiFileImpl;
import com.intellij.lang.puppet.psi.PuppetTypeDefinition;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetTypeStubIndex;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.ResolveResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PuppetTypeDefinitionReference extends PuppetPolyVariantCachingReferenceWithFullQualifiedName<PsiElement> {
  private final boolean myIncludeDataTypes;

  public PuppetTypeDefinitionReference(PsiElement psiElement, TextRange range, String fullQualifiedName) {
    this(psiElement, range, fullQualifiedName, false);
  }

  public PuppetTypeDefinitionReference(PsiElement psiElement, TextRange range, String fullQualifiedName, boolean includeDataTypes) {
    super(psiElement, range, fullQualifiedName);
    myIncludeDataTypes = includeDataTypes;
  }

  @Override
  protected ResolveResult @NotNull [] resolveInner(boolean incompleteCode) {
    if (StringUtil.isEmpty(myFullQualifiedName)) {
      return ResolveResult.EMPTY_ARRAY;
    }

    String lowerCasedName = StringUtil.toLowerCase(myFullQualifiedName);

    PsiElement element = getElement();
    final List<PsiElement> elements = new ArrayList<>(PuppetRubyPluginsIndex.findElementsByKey(
      PuppetRubyPluginsIndex.SymbolType.TYPE,
      lowerCasedName,
      element.getProject(),
      element.getResolveScope()));

    boolean gotRubyDefinedTypes = !elements.isEmpty();

    for (PuppetTypeDefinition typeDefinition : PuppetTypeStubIndex.getInstance().find(myFullQualifiedName, element)) {
      if (!gotRubyDefinedTypes || !PuppetPsiFileImpl.isInBuiltInStubsFile(typeDefinition)) {
        elements.add(typeDefinition);
      }
    }

    if (myIncludeDataTypes) {
      PsiElement typeElement =
        PuppetDataTypesManager.getDataTypeLightElementByName(myElement.getProject(), StringUtil.toLowerCase(myFullQualifiedName));
      if (typeElement != null) {
        elements.add(typeElement);
      }
    }

    return PsiElementResolveResult.createResults(elements);
  }

  @Override
  public @NotNull String getPresentableName() {
    return PuppetBundle.message("puppet.type.names.resource_definition");
  }
}
