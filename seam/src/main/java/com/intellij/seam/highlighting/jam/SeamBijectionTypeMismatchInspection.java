package com.intellij.seam.highlighting.jam;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiType;
import com.intellij.seam.model.jam.SeamJamComponent;
import com.intellij.seam.model.jam.bijection.SeamJamBijection;
import com.intellij.seam.model.jam.bijection.SeamJamInjection;
import com.intellij.seam.model.jam.bijection.SeamJamOutjection;
import com.intellij.seam.resources.SeamInspectionBundle;
import com.intellij.seam.utils.SeamCommonUtils;
import com.intellij.seam.utils.beans.ContextVariable;

public class SeamBijectionTypeMismatchInspection extends SeamJamModelInspectionBase {
  // this classes loaded with reflection in seam sources
  // (for instance, in org.jboss.seam.persistence.HibernatePersistenceProvider:
  // org.hibernate.search.jpa.FullTextEntityManager is loaded as implemntation of javax.persistence.EntityManager)
  final static String[] myHardCodedClasses = new String[] {"javax.persistence.EntityManager"};

  @Override
  protected void checkSeamJamComponent(final SeamJamComponent jamComponent, final ProblemsHolder holder) {
    final Module module = jamComponent.getModule();
    for (SeamJamInjection injection : jamComponent.getInjections()) {
      checkIncorrectContextVariableType(holder, injection, module);
    }

    for (SeamJamOutjection outjection : jamComponent.getOutjections()) {
      checkIncorrectContextVariableType(holder, outjection, module);
    }
  }

  private static void checkIncorrectContextVariableType(final ProblemsHolder holder,
                                                        final SeamJamBijection bijection,
                                                        final Module module) {
    final PsiType type = bijection.getType();
    if (type == null) return;

    String variableName = bijection.getName();
    if (variableName != null && !SeamCommonUtils.isElText(variableName)) {
      final ContextVariable contextVariable = SeamCommonUtils.getContextVariable(variableName, module);
      if (contextVariable == null) return;

      final PsiType contextVariableType = contextVariable.getType();

      if (!PsiType.VOID.equals(contextVariableType) && !isAssignable(type, contextVariableType)) {
        holder.registerProblem(bijection.getIdentifyingAnnotation(), SeamInspectionBundle.message(
            "bijection.context.variable.type.mismatch", contextVariableType.getPresentableText(), type.getPresentableText()));
      }
    }
  }

  private static boolean isAssignable(final PsiType type, final PsiType contextVariableType) {
    if (type.isAssignableFrom(contextVariableType)) return true;
    for (String aClass : myHardCodedClasses) {
      if (aClass.equals(contextVariableType.getCanonicalText())) {
         return contextVariableType.isAssignableFrom(type);
      }
    }
    return false;
  }
}

