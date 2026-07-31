package com.intellij.gwt.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.uiBinder.UiBinderUtil;
import com.intellij.gwt.uiBinder.mapping.UiBinderMappingService;
import com.intellij.java.syntax.parser.JavaKeywords;
import com.intellij.javaee.model.annotations.AnnotationModelUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class GwtUiFieldAssignmentInspection extends BaseGwtInspection {

  @Override
  public ProblemDescriptor[] checkClass(@NotNull PsiClass aClass, final @NotNull InspectionManager manager, final boolean isOnTheFly) {
    if (!shouldCheck(aClass) || UiBinderMappingService.getUiXmlFilesForClass(aClass).isEmpty()) return null;

    final List<ProblemDescriptor> problems = new SmartList<>();
    aClass.accept(new JavaRecursiveElementWalkingVisitor() {
      @Override
      public void visitAssignmentExpression(@NotNull PsiAssignmentExpression expression) {
        final PsiExpression left = expression.getLExpression();
        if (left instanceof PsiReferenceExpression) {
          final PsiElement element = ((PsiReferenceExpression)left).resolve();
          if (element instanceof PsiField field) {
            final PsiModifierList modifierList = field.getModifierList();
            if (modifierList != null) {
              final PsiAnnotation annotation = modifierList.findAnnotation(UiBinderUtil.UI_FIELD_ANNOTATION);
              if (annotation != null) {
                final Boolean provided = AnnotationModelUtil.getBooleanValue(annotation, UiBinderUtil.PROVIDED_ANNOTATION_ATTRIBUTE, false).getValue();
                if (provided == null || !provided) {
                  final String message = GwtBundle.message("inspection.message.assignment.to.uifield.annotated.field.will.be.ignored");
                  final SetProvidedAttributeQuickFix fix = new SetProvidedAttributeQuickFix(annotation);
                  problems.add(manager.createProblemDescriptor(expression, message, fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly));
                }
              }
            }
          }
        }
      }

      @Override
      public void visitReferenceExpression(@NotNull PsiReferenceExpression expression) {
      }
    });
    return problems.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }

  @Override
  public @NotNull HighlightDisplayLevel getDefaultLevel() {
    return HighlightDisplayLevel.WARNING;
  }

  private static final class SetProvidedAttributeQuickFix extends BaseGwtLocalQuickFixOnPsiElement {
    private SetProvidedAttributeQuickFix(PsiAnnotation annotation) {
      super(getQuickFixName(), getQuickFixName(), annotation);
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
      if (!(startElement instanceof PsiAnnotation psiAnnotation)) return;

      PsiExpression trueLiteral = JavaPsiFacade.getElementFactory(project).createExpressionFromText(JavaKeywords.TRUE, null);
      psiAnnotation.setDeclaredAttributeValue(UiBinderUtil.PROVIDED_ANNOTATION_ATTRIBUTE, trueLiteral);
    }

    private static @Nls String getQuickFixName() {
      return GwtBundle.message("quickfix.name.set.provided.to.true");
    }
  }
}
