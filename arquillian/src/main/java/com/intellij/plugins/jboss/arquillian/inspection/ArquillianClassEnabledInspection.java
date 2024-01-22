package com.intellij.plugins.jboss.arquillian.inspection;

import com.intellij.codeInsight.intention.AddAnnotationFix;
import com.intellij.codeInsight.intention.QuickFixFactory;
import com.intellij.codeInspection.*;
import com.intellij.plugins.jboss.arquillian.ArquillianBundle;
import com.intellij.plugins.jboss.arquillian.constants.ArquillianConstants;
import com.intellij.plugins.jboss.arquillian.utils.ArquillianUtils;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ArquillianClassEnabledInspection extends AbstractBaseJavaLocalInspectionTool {

  static private ProblemDescriptor createTestngProblemDescriptor(@NotNull PsiClass aClass,
                                                                 @NotNull InspectionManager manager,
                                                                 boolean isOnTheFly) {
    return manager.createProblemDescriptor(
      aClass.getNameIdentifier() == null ? aClass : aClass.getNameIdentifier(),
      ArquillianBundle.message("arquillian.testng.should.extend.arquillian"),
      isOnTheFly,
      new LocalQuickFix[]{createTestngQuickFix(aClass), createMakeClassAbstractQuickFix(aClass)},
      ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
  }

  private static LocalQuickFix createTestngQuickFix(@NotNull PsiClass aClass) {
    return QuickFixFactory.getInstance().createExtendsListFix(
      aClass,
      ArquillianUtils.getTestngArquillianType(aClass.getProject()),
      true);
  }

  static private ProblemDescriptor createJUnitProblemDescriptor(@NotNull PsiClass aClass,
                                                                @NotNull InspectionManager manager,
                                                                boolean isOnTheFly) {
    return manager.createProblemDescriptor(
      aClass.getNameIdentifier() == null ? aClass : aClass.getNameIdentifier(),
      ArquillianBundle.message("arquillian.junit.run.with.required"),
      isOnTheFly,
      new LocalQuickFix[]{createJUnitQuickFix(aClass), createMakeClassAbstractQuickFix(aClass)},
      ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
  }

  private static LocalQuickFix createMakeClassAbstractQuickFix(@NotNull PsiClass aClass) {
    if (aClass.getModifierList() == null) {
      return null;
    }
    return QuickFixFactory.getInstance().createModifierListFix(aClass, PsiModifier.ABSTRACT, true, true);
  }

  private static LocalQuickFix createJUnitQuickFix(@NotNull PsiClass aClass) {
    PsiAnnotation annotation = JavaPsiFacade.getInstance(aClass.getProject()).getElementFactory()
      .createAnnotationFromText(
        "@" + ArquillianConstants.JUNIT_RUN_WITH_CLASS + "(" + ArquillianConstants.JUNIT_ARQUILLIAN_CLASS + ".class)", aClass);
    return new AddAnnotationFix(ArquillianConstants.JUNIT_RUN_WITH_CLASS, aClass, annotation.getParameterList().getAttributes());
  }

  @Override
  public ProblemDescriptor @Nullable [] checkClass(@NotNull PsiClass aClass, @NotNull InspectionManager manager, boolean isOnTheFly) {
    if (ArquillianUtils.getAnnotatedMethods(aClass, ArquillianConstants.DEPLOYMENT_CLASS).isEmpty()) {
      return null;
    }
    if (aClass.getModifierList() != null && aClass.getModifierList().hasExplicitModifier(PsiModifier.ABSTRACT)) {
      return null;
    }
    List<ProblemDescriptor> problemDescriptors = new ArrayList<>();
    if (!ArquillianUtils.getAnnotatedMethods(aClass, ArquillianConstants.JUNIT_TEST_CLASS).isEmpty()
        && !ArquillianUtils.isJunitArquillianEnabled(aClass)) {
      problemDescriptors.add(createJUnitProblemDescriptor(aClass, manager, isOnTheFly));
    }
    if (!ArquillianUtils.getAnnotatedMethods(aClass, ArquillianConstants.TESTNG_TEST_CLASS).isEmpty()
        && !ArquillianUtils.isTestngArquillianEnabled(aClass)) {
      problemDescriptors.add(createTestngProblemDescriptor(aClass, manager, isOnTheFly));
    }
    return problemDescriptors.isEmpty()
           ? null
           : problemDescriptors.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }
}
