package com.intellij.lang.puppet.psi.references;

import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.psi.PuppetClassDefinition;
import com.intellij.lang.puppet.psi.PuppetResourceLikeClassDescription;
import com.intellij.lang.puppet.psi.resolve.PuppetNamedPsiElementProcessor;
import com.intellij.lang.puppet.psi.stubs.indices.PuppetClassStubsIndex;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public class PuppetClassParameterReference extends PuppetParameterReferenceBase {

  public PuppetClassParameterReference(PsiElement psiElement) {
    super(psiElement);
  }

  @Override
  protected boolean processCandidates(@NotNull String paramName, @NotNull PuppetNamedPsiElementProcessor processor) {
    PuppetResourceLikeClassDescription classDescription = PsiTreeUtil.getParentOfType(myElement, PuppetResourceLikeClassDescription.class);
    assert classDescription != null;

    for (String className : classDescription.getNamesList()) {
      for (PuppetClassDefinition classDefinition : PuppetClassStubsIndex.getInstance().find(className, myElement)) {
        classDefinition.processParametersDeclarations(variable -> {
          String variableName = variable.getName();
          if (StringUtil.isNotEmpty(variableName)) {
            processor.executeWithName(variableName, variable);
          }
          return true;
        });
      }
    }

    return true;
  }

  @Override
  public @NotNull String getPresentableName() {
    return PuppetBundle.message("puppet.type.names.class.parameter");
  }
}
