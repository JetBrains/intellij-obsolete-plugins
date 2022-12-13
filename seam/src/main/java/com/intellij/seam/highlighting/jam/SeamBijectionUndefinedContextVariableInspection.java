package com.intellij.seam.highlighting.jam;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.module.Module;
import com.intellij.seam.model.SeamComponentScope;
import com.intellij.seam.model.jam.SeamJamComponent;
import com.intellij.seam.model.jam.bijection.SeamJamBijection;
import com.intellij.seam.model.jam.bijection.SeamJamInjection;
import com.intellij.seam.model.jam.bijection.SeamJamOutjection;
import com.intellij.seam.resources.SeamInspectionBundle;
import com.intellij.seam.utils.SeamCommonUtils;
import org.jetbrains.annotations.NotNull;

public class SeamBijectionUndefinedContextVariableInspection  extends SeamJamModelInspectionBase {
  @Override
  protected void checkSeamJamComponent(final SeamJamComponent seamJamComponent, final ProblemsHolder holder) {
    final Module module = seamJamComponent.getModule();
    for (SeamJamInjection injection : seamJamComponent.getInjections()) {
      checkInjectionContextVariableExisted(holder, injection, module);
    }

    for (SeamJamOutjection outjection : seamJamComponent.getOutjections()) {
      checkOutjectionContextVariableExisted(holder, outjection, module);
    }
  }

  private static void checkInjectionContextVariableExisted(final ProblemsHolder holder, final SeamJamInjection baseSeamInjection,
                                                    final Module module) {
      checkContextVariableExisted(holder, baseSeamInjection, module);
  }

  private static void checkOutjectionContextVariableExisted(final ProblemsHolder holder, final SeamJamOutjection outjection,
                                                            final Module module) {
    final SeamComponentScope scope = outjection.getScope();
    if (scope != null && scope != SeamComponentScope.UNSPECIFIED) return;

    checkContextVariableExisted(holder, outjection, module);
  }

  private static void checkContextVariableExisted(final ProblemsHolder holder,
                                                  final SeamJamBijection bijection,
                                                  final Module module) {
    String variableName = bijection.getName();
    if (variableName != null && !SeamCommonUtils.isElText(variableName)) {
      if (!isContextVariableExisted(variableName, module)) {
        holder.registerProblem(bijection.getIdentifyingAnnotation(),
                               SeamInspectionBundle.message("bijection.undefined.context.variable", variableName));
      }
    }
  }

  private static boolean isContextVariableExisted(@NotNull final String variableName, final Module module) {
    return SeamCommonUtils.getContextVariable(variableName, module) != null;
  }
}
