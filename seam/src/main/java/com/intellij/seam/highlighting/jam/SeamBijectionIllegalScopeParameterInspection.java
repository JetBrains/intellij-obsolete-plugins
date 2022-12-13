package com.intellij.seam.highlighting.jam;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.module.Module;
import com.intellij.seam.model.SeamComponentScope;
import com.intellij.seam.model.jam.SeamJamComponent;
import com.intellij.seam.model.jam.SeamJamFactory;
import com.intellij.seam.model.jam.bijection.SeamJamBijection;
import com.intellij.seam.model.jam.bijection.SeamJamInjection;
import com.intellij.seam.model.jam.bijection.SeamJamOutjection;
import com.intellij.seam.resources.SeamInspectionBundle;
import com.intellij.seam.utils.SeamCommonUtils;
import com.intellij.seam.utils.beans.ContextVariable;
import org.jetbrains.annotations.Nullable;

public class SeamBijectionIllegalScopeParameterInspection extends SeamJamModelInspectionBase {

  @Override
  protected void checkSeamJamComponent(final SeamJamComponent seamJamComponent, final ProblemsHolder holder) {
    final Module module = seamJamComponent.getModule();
    for (SeamJamInjection injection : seamJamComponent.getInjections()) {
      checkInjectionIllegalScopeParameter(holder, injection);
    }

    for (SeamJamOutjection outjection : seamJamComponent.getOutjections()) {
      checkOutjectionIllegalScopeParameter(holder, outjection, module);
    }
  }

  private static void checkInjectionIllegalScopeParameter(final ProblemsHolder holder, final SeamJamInjection injection) {
    final SeamComponentScope scope = injection.getScope();
    if (scope == null || scope.isEqual(SeamComponentScope.UNSPECIFIED)) return;
    if (injection.isCreate()) {
      holder.registerProblem(injection.getIdentifyingAnnotation(),
                             SeamInspectionBundle.message("bijection.injection.illegal.scope.declaration"));
    }
  }

  protected static void checkOutjectionIllegalScopeParameter(final ProblemsHolder holder,
                                                             final SeamJamBijection outjection,
                                                             final Module module) {
    final SeamComponentScope scope = outjection.getScope();
    if (scope == null || scope.isEqual(SeamComponentScope.UNSPECIFIED)) return;

    String variableName = outjection.getName();
    if (variableName != null && !SeamCommonUtils.isElText(variableName)) {
      final ContextVariable contextVariable = getContextVariable(variableName, module);
      if (contextVariable != null && !outjection.equals(contextVariable.getModelElement())) {
        if (!(contextVariable.getModelElement() instanceof SeamJamFactory)) {  // IDEADEV-26171
          holder.registerProblem(outjection.getIdentifyingAnnotation(),
                                 SeamInspectionBundle.message("bijection.outjection.illegal.scope.declaration"));
        }
      }
    }
  }

  @Nullable
  protected static ContextVariable getContextVariable(final String variableName, final Module module) {
    for (ContextVariable variable : SeamCommonUtils.getSeamContextVariablesWithDependencies(module)) {
      if (variableName.equals(variable.getName())) return variable;
    }
    return null;
  }
}
