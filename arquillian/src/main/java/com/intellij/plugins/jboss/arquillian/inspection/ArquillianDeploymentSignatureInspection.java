package com.intellij.plugins.jboss.arquillian.inspection;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.QuickFixFactory;
import com.intellij.codeInspection.*;
import com.intellij.plugins.jboss.arquillian.ArquillianBundle;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.plugins.jboss.arquillian.constants.ArquillianConstants.DEPLOYMENT_CLASS;

public class ArquillianDeploymentSignatureInspection extends AbstractBaseJavaLocalInspectionTool {
  static private void checkModifiersAndReturnType(@NotNull final PsiMethod method,
                                                  @NotNull final PsiAnnotation deploymentAnnotation,
                                                  @NotNull ProblemsHolder holder) {
    ArrayList<LocalQuickFix> fixes = new ArrayList<>();
    if (!method.hasModifierProperty(PsiModifier.STATIC)) {
      fixes.add(QuickFixFactory.getInstance().createModifierListFix(method, PsiModifier.STATIC, true, true));
    }
    if (!method.hasModifierProperty(PsiModifier.PUBLIC)) {
      fixes.add(QuickFixFactory.getInstance().createModifierListFix(method, PsiModifier.PUBLIC, true, true));
    }
    if (fixes.isEmpty()) {
      return;
    }
    holder.registerProblem(
      deploymentAnnotation,
      ArquillianBundle.message("arquillian.deployment.wrong.signature"),
      fixes.toArray(LocalQuickFix.EMPTY_ARRAY));
  }

  private static void checkWrongParameterCount(@NotNull final PsiMethod method,
                                               @NotNull final PsiAnnotation deploymentAnnotation,
                                               @NotNull final ProblemsHolder holder) {
    RemoveAnnotationQuickFix removeAnnotationQuickFix = new RemoveAnnotationQuickFix(deploymentAnnotation, method);
    IntentionAction changeMethodSignatureFix = QuickFixFactory.getInstance().createChangeMethodSignatureFromUsageFix(
      method,
      PsiExpression.EMPTY_ARRAY,
      EmptySubstitutor.getInstance(),
      method,
      false,
      111);

    holder.registerProblem(
      deploymentAnnotation,
      ArquillianBundle.message("arquillian.deployment.wrong.signature"),
      new IntentionWrapper(changeMethodSignatureFix),
      removeAnnotationQuickFix);
  }

  @Override
  public ProblemDescriptor[] checkMethod(@NotNull final PsiMethod method, @NotNull InspectionManager manager, boolean isOnTheFly) {
    PsiAnnotation deploymentAnnotation = AnnotationUtil.findAnnotation(method, DEPLOYMENT_CLASS);
    if (deploymentAnnotation == null) {
      return null;
    }
    ProblemsHolder holder = new ProblemsHolder(manager, method.getContainingFile(), isOnTheFly);

    if (method.getParameterList().getParametersCount() > 0) {
      checkWrongParameterCount(method, deploymentAnnotation, holder);
    }
    checkModifiersAndReturnType(method, deploymentAnnotation, holder);
    final List<ProblemDescriptor> problemDescriptors = holder.getResults();
    return problemDescriptors.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }
}
