package com.intellij.plugins.jboss.arquillian.inspection;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.plugins.jboss.arquillian.ArquillianBundle;
import com.intellij.plugins.jboss.arquillian.constants.ArquillianConstants;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ArquillianTooManyDeploymentInspection extends ArquillianDeploymentCountInspectionBase {
  @Override
  protected void checkDeploymentMethods(@NotNull PsiClass aClass,
                                        @NotNull List<PsiMethod> deploymentMethods,
                                        @NotNull ProblemsHolder holder) {
    for (PsiMethod method : deploymentMethods) {
      if (!method.getContainingFile().equals(aClass.getContainingFile())) {
        continue;
      }

      PsiAnnotation annotation = AnnotationUtil.findAnnotation(method, ArquillianConstants.DEPLOYMENT_CLASS);
      holder.registerProblem(holder.getManager().createProblemDescriptor(
        annotation != null ? annotation : method.getNameIdentifier() != null ? method.getNameIdentifier() : method,
        ArquillianBundle.message("arquillian.deployment.too.many"),
        holder.isOnTheFly(),
        LocalQuickFix.EMPTY_ARRAY,
        ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
    }
  }

  @Override
  protected boolean wouldLikeToCheckMethodCount(int count) {
    return count > 1;
  }
}
