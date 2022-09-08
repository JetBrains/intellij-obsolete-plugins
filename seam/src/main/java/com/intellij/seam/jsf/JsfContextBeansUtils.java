package com.intellij.seam.jsf;

import com.intellij.javaee.el.util.ELImplicitVariable;
import com.intellij.javaee.model.psi.JavaeeImplicitVariable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.seam.utils.SeamCommonUtils;
import com.intellij.seam.utils.beans.ContextVariable;
import com.intellij.seam.utils.beans.DomFactoryContextVariable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class JsfContextBeansUtils {
  private JsfContextBeansUtils() {
  }

  public static Collection<JavaeeImplicitVariable> getJspImplicitVariables(final String name, final PsiFile file) {
    final Map<String, JavaeeImplicitVariable> resultVars = new HashMap<>();

    if (file != null) {
      Module module = ModuleUtilCore.findModuleForPsiElement(file);
      if (module != null) {

        for (ContextVariable contextVariable : SeamCommonUtils.getSeamContextVariablesWithDependencies(module)) {
          if (name == null || contextVariable.getName().startsWith(name)) {
            addSeamImplicitVariable(contextVariable.getModelElement().getIdentifyingPsiElement(), contextVariable.getName(), resultVars,
                                    getType(contextVariable), file);
          }
        }
      }
    }
    return resultVars.values();
  }

  private static PsiType getType(ContextVariable variable) {
    if (variable instanceof DomFactoryContextVariable) {
      DomFactoryContextVariable factoryContextVariable = (DomFactoryContextVariable)variable;

      PsiType expressionType = factoryContextVariable.getELExpressionType();
      return expressionType == null ? variable.getType() : expressionType;
    }

    return variable.getType();
  }

  public static Collection<JavaeeImplicitVariable> getJspImplicitVariables(final PsiFile file) {
    final Map<String, JavaeeImplicitVariable> resultVars = new HashMap<>();
    if (file != null) {
      Module module = ModuleUtilCore.findModuleForPsiElement(file);
      if (module != null && SeamCommonUtils.isSeamFacetDefined(module)) {
        addPredefinedSeamVariable(file, resultVars, module);

        for (ContextVariable contextVariable : SeamCommonUtils.getSeamContextVariablesWithDependencies(module)) {
          addSeamImplicitVariable(contextVariable.getModelElement().getIdentifyingPsiElement(), contextVariable.getName(), resultVars,
                                  contextVariable.getType(), file);
        }
      }
    }
    return resultVars.values();
  }

  private static void addPredefinedSeamVariable(@NotNull PsiFile file, @NotNull Map<String, JavaeeImplicitVariable> vars, Module module) {
    addMessagesComponent(file, vars, module);
  }

  private static void addMessagesComponent(PsiFile file, Map<String, JavaeeImplicitVariable> vars, @NotNull Module module) {
    final String varName = "messages";
    vars.put(varName, new ELImplicitVariable(file, varName,
                                             JavaPsiFacade.getInstance(file.getProject()).getElementFactory().createTypeByFQClassName(
                                               CommonClassNames.JAVA_UTIL_PROPERTY_RESOURCE_BUNDLE,
                                               GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)),
                                             file));
  }

  private static void addSeamImplicitVariable(final PsiElement psiElement,
                                              @Nullable final String name,
                                              final Map<String, JavaeeImplicitVariable> result,
                                              @Nullable final PsiType type,
                                              final PsiFile file) {
    if (name == null || name.length() == 0 || type == null || result.containsKey(name)) return;

    result.put(name, new ELImplicitVariable(file, name, type, psiElement, ELImplicitVariable.NESTED_RANGE));
    if (name.contains(".")) {
      final String otherName = formatName(name);
      result.put(otherName, new ELImplicitVariable(file, otherName, type, psiElement, ELImplicitVariable.NESTED_RANGE));
    }
  }

  private static String formatName(String name) {
    return name.replace('.', '$');
  }
}
