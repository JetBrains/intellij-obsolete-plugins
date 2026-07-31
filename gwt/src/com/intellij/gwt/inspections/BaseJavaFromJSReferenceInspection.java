package com.intellij.gwt.inspections;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.gwt.jsinject.JSGwtReferenceExpressionImpl;
import com.intellij.gwt.jsinject.parser.GwtLanguageDialect;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseJavaFromJSReferenceInspection extends BaseGwtInspection {
  @Override
  public ProblemDescriptor @Nullable [] checkFile(final @NotNull PsiFile file, final @NotNull InspectionManager manager, final boolean isOnTheFly) {
    if (!GwtLanguageDialect.GWT_DIALECT.equals(file.getLanguage())) {
      return null;
    }

    final List<ProblemDescriptor> problems = new ArrayList<>();
    file.accept(new PsiRecursiveElementWalkingVisitor() {
      @Override public void visitElement(final @NotNull PsiElement element) {
        if (element instanceof JSGwtReferenceExpressionImpl) {
          checkReferenceExpression((JSGwtReferenceExpressionImpl)element, problems, manager, isOnTheFly);
        }
        else {
          super.visitElement(element);
        }
      }
    });
    return problems.toArray(ProblemDescriptor.EMPTY_ARRAY);
  }

  protected abstract void checkReferenceExpression(JSGwtReferenceExpressionImpl element,
                                                   List<ProblemDescriptor> problems,
                                                   InspectionManager manager,
                                                   boolean isOnTheFly);
}
