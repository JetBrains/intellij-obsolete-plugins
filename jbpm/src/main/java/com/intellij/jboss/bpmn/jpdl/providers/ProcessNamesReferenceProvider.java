package com.intellij.jboss.bpmn.jpdl.providers;

import com.intellij.jboss.bpmn.jpdl.model.xml.ProcessDefinition;
import com.intellij.jboss.bpmn.jpdl.utils.JpdlCommonUtils;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.pom.references.PomService;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import com.intellij.util.xml.DomTarget;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ProcessNamesReferenceProvider extends PsiReferenceProvider {

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull final ProcessingContext context) {
    if (element instanceof PsiLiteralExpression literalExpression) {
      final Module module = ModuleUtilCore.findModuleForPsiElement(element);
      if (module != null && literalExpression.getValue() instanceof String) {
        return new PsiReference[]{new JpdlProcessPsiReference(literalExpression, module)};
      }
    }
    return PsiReference.EMPTY_ARRAY;
  }

  private static class JpdlProcessPsiReference extends PsiReferenceBase<PsiLiteralExpression> {
    private final Module myModule;

    JpdlProcessPsiReference(PsiLiteralExpression literalExpression, Module module) {
      super(literalExpression, false);
      myModule = module;
    }

    @Override
    public boolean isSoft() {
      return false;
    }

    @Override
    public PsiElement resolve() {
      final String value = getValue();
      for (ProcessDefinition definition : JpdlCommonUtils.getProcessDefinitions(myModule)) {
        if (value.equals(definition.getName().getStringValue())) {
          return PomService.convertToPsi(DomTarget.getTarget(definition));
        }
      }
      return null;
    }

    @Override
    public Object @NotNull [] getVariants() {
      final List<String> names = JpdlCommonUtils.getProcessNames(myModule);

      return names.toArray();
    }
  }
}
