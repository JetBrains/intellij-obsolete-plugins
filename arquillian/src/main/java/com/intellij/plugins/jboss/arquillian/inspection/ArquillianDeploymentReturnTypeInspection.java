package com.intellij.plugins.jboss.arquillian.inspection;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.intention.QuickFixFactory;
import com.intellij.codeInspection.*;
import com.intellij.plugins.jboss.arquillian.ArquillianBundle;
import com.intellij.plugins.jboss.arquillian.constants.ArquillianConstants;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.NotNull;

public class ArquillianDeploymentReturnTypeInspection extends AbstractBaseJavaLocalInspectionTool {
  @Override
  public ProblemDescriptor[] checkMethod(@NotNull final PsiMethod method, @NotNull InspectionManager manager, boolean isOnTheFly) {
    if (!AnnotationUtil.isAnnotated(method, ArquillianConstants.DEPLOYMENT_CLASS, 0)) {
      return null;
    }
    final PsiType shrinkWrapArchiveType = PsiType.getTypeByName(
      ArquillianConstants.JAVA_ARCHIVE_CLASS,
      method.getProject(),
      GlobalSearchScope.allScope(method.getProject()));
    if (method.getReturnType() != null && TypeConversionUtil.isAssignable(shrinkWrapArchiveType, method.getReturnType())) {
      return null;
    }
    LocalQuickFix returnTypeQuickFix = QuickFixFactory.getInstance().createMethodReturnFix(method, shrinkWrapArchiveType, false);

    ProblemDescriptor problemDescriptor = manager.createProblemDescriptor(
      method.getReturnTypeElement() == null ? method : method.getReturnTypeElement(),
      ArquillianBundle.message("arquillian.deployment.wrong.return.type"),
      isOnTheFly,
      new LocalQuickFix[]{returnTypeQuickFix},
      ProblemHighlightType.GENERIC_ERROR_OR_WARNING);

    return new ProblemDescriptor[]{problemDescriptor};
  }
}
