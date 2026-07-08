package com.intellij.lang.puppet.psi.references;

import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.ide.navigation.plugins.ruby.PuppetRubyPluginsIndex;
import com.intellij.lang.puppet.psi.PuppetResourceDeclarationBase;
import com.intellij.lang.puppet.psi.PuppetTypeDefinition;
import com.intellij.lang.puppet.psi.PuppetVariable;
import com.intellij.lang.puppet.psi.resolve.PuppetNamedPsiElementProcessor;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetTypeStubIndex;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import static com.intellij.lang.puppet.util.PuppetQualifiedNamesUtil.SEPARATOR;

public class PuppetTypeParameterReference extends PuppetParameterReferenceBase {

  public PuppetTypeParameterReference(PsiElement psiElement) {
    super(psiElement);
  }

  @Override
  protected boolean processCandidates(@NotNull String paramName, @NotNull PuppetNamedPsiElementProcessor processor) {
    PuppetResourceDeclarationBase resourceDeclaration = PsiTreeUtil.getParentOfType(myElement, PuppetResourceDeclarationBase.class);
    assert resourceDeclaration != null;

    String typeName = resourceDeclaration.getEffectiveTypeName();
    if (typeName == null) {
      return true;
    }

    typeName = StringUtil.trimStart(typeName, SEPARATOR);

    if (StringUtil.isEmpty(typeName)) {
      return true;
    }

    boolean[] foundInRuby = new boolean[]{false};

    PuppetRubyPluginsIndex.processElementParameters(
      PuppetRubyPluginsIndex.SymbolType.TYPE,
      typeName,
      resourceDeclaration.getProject(),
      resourceDeclaration.getResolveScope(),
      (name, element) -> {
        foundInRuby[0] = foundInRuby[0] || StringUtil.equals(paramName, name);
        processor.executeWithName(name, element);
      }
    );

    if (foundInRuby[0]) {
      return true;
    }

    PsiElementProcessor<PuppetVariable> variableProcessor = variable -> {
      String variableName = variable.getName();
      if (StringUtil.isNotEmpty(variableName)) {
        processor.executeWithName(variableName, variable);
      }
      return true;
    };


    for (PuppetTypeDefinition typeDefinition : PuppetTypeStubIndex.getInstance().find(typeName, myElement)) {
      typeDefinition.processParametersDeclarations(variableProcessor);
    }

    for (PuppetTypeDefinition typeDefinition : PuppetTypeStubIndex.getMetaparametersContainingTypes(myElement)) {
      typeDefinition.processParametersDeclarations(variableProcessor);
    }

    return true;
  }

  @Override
  public @NotNull String getPresentableName() {
    return PuppetBundle.message("puppet.type.names.type.parameter");
  }
}
