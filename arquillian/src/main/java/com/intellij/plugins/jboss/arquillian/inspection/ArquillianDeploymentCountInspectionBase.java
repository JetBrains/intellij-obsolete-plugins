package com.intellij.plugins.jboss.arquillian.inspection;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.plugins.jboss.arquillian.constants.ArquillianConstants;
import com.intellij.plugins.jboss.arquillian.utils.ArquillianUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class ArquillianDeploymentCountInspectionBase extends AbstractBaseJavaLocalInspectionTool {
  @Override
  public ProblemDescriptor @Nullable [] checkClass(@NotNull PsiClass aClass, @NotNull InspectionManager manager, boolean isOnTheFly) {
    if (!ArquillianUtils.isTestngArquillianEnabled(aClass) && !ArquillianUtils.isJunitArquillianEnabled(aClass)) {
      return null;
    }
    List<PsiMethod> deploymentMethods = ArquillianUtils.getAnnotatedMethods(aClass, ArquillianConstants.DEPLOYMENT_CLASS);
    if (!wouldLikeToCheckMethodCount(deploymentMethods.size())) {
      return null;
    }

    ProblemsHolder holder = new ProblemsHolder(manager, aClass.getContainingFile(), isOnTheFly);
    checkDeploymentMethods(aClass, deploymentMethods, holder);
    final List<ProblemDescriptor> problemDescriptors = holder.getResults();
    return problemDescriptors.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }

  protected abstract void checkDeploymentMethods(@NotNull PsiClass aClass,
                                                 @NotNull List<PsiMethod> deploymentMethods,
                                                 @NotNull ProblemsHolder holder);

  protected abstract boolean wouldLikeToCheckMethodCount(int count);
}
