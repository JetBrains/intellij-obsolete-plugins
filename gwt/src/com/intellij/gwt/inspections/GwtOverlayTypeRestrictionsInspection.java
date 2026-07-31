package com.intellij.gwt.inspections;

import com.intellij.codeInsight.daemon.impl.quickfix.AddDefaultConstructorFix;
import com.intellij.codeInsight.intention.QuickFixFactory;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.gwt.GwtBundle;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.psi.PsiModifier.FINAL;
import static com.intellij.psi.PsiModifier.PRIVATE;
import static com.intellij.psi.PsiModifier.PROTECTED;
import static com.intellij.psi.PsiModifier.STATIC;

public final class GwtOverlayTypeRestrictionsInspection extends BaseGwtInspection {
  private static final String JAVASCRIPT_OBJECT_CLASS_NAME = "com.google.gwt.core.client.JavaScriptObject";

  @Override
  public ProblemDescriptor[] checkClass(@NotNull PsiClass aClass, @NotNull InspectionManager manager, boolean isOnTheFly) {
    if (!shouldCheck(aClass) || aClass instanceof PsiTypeParameter) return null;

    if (!InheritanceUtil.isInheritor(aClass, true, JAVASCRIPT_OBJECT_CLASS_NAME)) return null;

    List<ProblemDescriptor> problems = new SmartList<>();
    checkConstructors(aClass, manager, isOnTheFly, problems);
    checkFields(aClass, manager, isOnTheFly, problems);
    checkInnerClasses(aClass, manager, isOnTheFly, problems);
    checkMethods(aClass, manager, isOnTheFly, problems);

    return !problems.isEmpty() ? problems.toArray(ProblemDescriptor.EMPTY_ARRAY) : null;
  }

  private static void checkMethods(PsiClass aClass, InspectionManager manager, boolean isOnTheFly, List<ProblemDescriptor> problems) {
    if (!aClass.hasModifierProperty(FINAL)) {
      for (PsiMethod method : aClass.getMethods()) {
        if (!method.isConstructor() && !method.hasModifierProperty(STATIC) && !method.hasModifierProperty(FINAL) && !method.hasModifierProperty(PRIVATE)) {
          final String message = GwtBundle.message("problem.description.instance.methods.in.overlay.type.must.be.final");
          final LocalQuickFix fix = QuickFixFactory.getInstance().createModifierListFix(method, FINAL, true, false);
          problems.add(manager.createProblemDescriptor(getElementToHighlight(method), message, fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly));
        }
      }
    }
  }

  private static void checkInnerClasses(PsiClass aClass, InspectionManager manager, boolean isOnTheFly, List<ProblemDescriptor> problems) {
    for (PsiClass innerClass : aClass.getInnerClasses()) {
      if (!innerClass.hasModifierProperty(STATIC)) {
        final String message = GwtBundle.message("problem.description.inner.classes.in.overlay.type.must.be.static");
        final LocalQuickFix fix = QuickFixFactory.getInstance().createModifierListFix(innerClass, STATIC, true, false);
        problems.add(manager.createProblemDescriptor(getElementToHighlight(innerClass), message, fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly));
      }
    }
  }

  private static void checkFields(PsiClass aClass, InspectionManager manager, boolean isOnTheFly, List<ProblemDescriptor> problems) {
    for (PsiField field : aClass.getFields()) {
      if (!field.hasModifierProperty(STATIC)) {
        final String message = GwtBundle.message("problem.description.overlay.type.cannot.have.instance.fields");
        final LocalQuickFix fix = QuickFixFactory.getInstance().createModifierListFix(field, STATIC, true, false);
        problems.add(manager.createProblemDescriptor(field.getNameIdentifier(), message, fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly));
      }
    }
  }

  private static void checkConstructors(PsiClass aClass, InspectionManager manager, boolean isOnTheFly, List<ProblemDescriptor> problems) {
    for (PsiMethod constructor : aClass.getConstructors()) {
      final PsiCodeBlock body = constructor.getBody();
      if (constructor.getParameterList().getParametersCount() > 0) {
        final String message = GwtBundle.message("problem.description.constructor.in.overlay.type.cannot.have.parameters");
        LocalQuickFix fix = null;
        problems.add(manager.createProblemDescriptor(getElementToHighlight(constructor), message, fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly));
      }
      else if (!constructor.hasModifierProperty(PROTECTED)) {
        final String message = GwtBundle.message("problem.description.constructor.in.overlay.type.must.be.protected");
        LocalQuickFix fix = QuickFixFactory.getInstance().createModifierListFix(constructor, PROTECTED, true, false);
        problems.add(manager.createProblemDescriptor(getElementToHighlight(constructor), message, fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly));
      }
      else if (body != null && body.getStatements().length > 0) {
        final String message = GwtBundle.message("problem.description.constructor.in.overlay.type.must.have.empty.body");
        LocalQuickFix fix = null;
        problems.add(manager.createProblemDescriptor(getElementToHighlight(constructor), message, fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly));
      }
    }
    if (aClass.getConstructors().length == 0) {
      final LocalQuickFix fix = LocalQuickFix.from(new AddDefaultConstructorFix(aClass, PROTECTED));
      final String message = GwtBundle.message("problem.description.overlay.type.must.have.protected.empty.no.arg.constructor");
      problems.add(manager.createProblemDescriptor(getElementToHighlight(aClass), message, fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly));
    }
  }
}
