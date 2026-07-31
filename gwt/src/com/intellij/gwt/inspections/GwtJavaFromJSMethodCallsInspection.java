package com.intellij.gwt.inspections;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.jsinject.GwtClassMemberReference;
import com.intellij.gwt.jsinject.JSGwtReferenceExpressionImpl;
import com.intellij.lang.javascript.psi.JSArgumentList;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSReferenceExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.psi.util.PsiFormatUtilBase;

import java.util.List;

public final class GwtJavaFromJSMethodCallsInspection extends BaseJavaFromJSReferenceInspection {
  @Override
  protected void checkReferenceExpression(JSGwtReferenceExpressionImpl element,
                                          List<ProblemDescriptor> problems,
                                          InspectionManager manager,
                                          boolean isOnTheFly) {
    final PsiReference[] references = element.getReferences();
    if (references.length == 0) return;

    final PsiReference lastRef = references[references.length - 1];
    if (!(lastRef instanceof GwtClassMemberReference)) return;

    final PsiElement resolved = lastRef.resolve();
    if (!(resolved instanceof PsiMethod method)) return;

    final PsiElement parent = element.getParent();
    if (!method.getModifierList().hasModifierProperty(PsiModifier.STATIC) && !method.isConstructor()) {
      if (parent instanceof JSReferenceExpression && ((JSReferenceExpression)parent).getQualifier() == null) {
        String message = GwtBundle.message("problem.description.cannot.call.instance.method.without.object.instance", PsiFormatUtil.formatMethod(method, PsiSubstitutor.EMPTY, PsiFormatUtilBase.SHOW_NAME | PsiFormatUtilBase.SHOW_CONTAINING_CLASS, 0));
        problems.add(manager.createProblemDescriptor(element, lastRef.getRangeInElement(), message,
                                                     ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                                     isOnTheFly));
      }
    }

    if (parent instanceof JSReferenceExpression) {
      final PsiElement grandParent = parent.getParent();
      if (grandParent instanceof JSCallExpression call) {
        final int expectedParameters = method.getParameterList().getParametersCount();
        final int actualParameters = call.getArguments().length;
        if (expectedParameters != actualParameters) {
          String message = GwtBundle.message("problem.description.incorrect.number.of.arguments.for.method", PsiFormatUtil
            .formatMethod(method, PsiSubstitutor.EMPTY,
                          PsiFormatUtilBase.SHOW_NAME | PsiFormatUtilBase.SHOW_CONTAINING_CLASS | PsiFormatUtilBase.SHOW_PARAMETERS,
                          PsiFormatUtilBase.SHOW_TYPE), expectedParameters, actualParameters);
          final JSArgumentList list = call.getArgumentList();
          problems.add(manager.createProblemDescriptor(list != null ? list : call, message, isOnTheFly, LocalQuickFix.EMPTY_ARRAY,
                                                       ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
        }
      }
    }
  }
}
