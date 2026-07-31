package com.intellij.gwt.inspections;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.sdk.GwtSdkUtil;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PsiMethodCallPattern;
import com.intellij.patterns.PsiMethodPattern;
import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.util.ProcessingContext;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.patterns.PsiJavaPatterns.psiElement;
import static com.intellij.patterns.PsiJavaPatterns.psiExpression;
import static com.intellij.patterns.PsiJavaPatterns.psiMethod;

public final class GwtCreateMethodCallInspection extends BaseGwtInspection {
  private static final Key<PsiExpression> GWT_CREATE_ARGUMENT_KEY = Key.create("GWT_CREATE_ARGUMENT");
  private static final PsiMethodCallPattern INCORRECT_CREATE_METHOD_CALL_PATTERN;

  static {
    final PsiMethodPattern createMethodPattern = psiMethod().withName("create").definedInClass(GwtSdkUtil.GWT_CLASS_NAME);
    INCORRECT_CREATE_METHOD_CALL_PATTERN =
      psiExpression()
        .methodCall(createMethodPattern)
        .withArguments(psiExpression().andNot(psiElement(PsiClassObjectAccessExpression.class)).save(GWT_CREATE_ARGUMENT_KEY));
  }
  
  @Override
  public ProblemDescriptor[] checkFile(@NotNull PsiFile file, final @NotNull InspectionManager manager, final boolean isOnTheFly) {
    if (!shouldCheck(file)) return null;
    
    final GwtModulesManager gwtModulesManager = GwtModulesManager.getInstance(file.getProject());
    final VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) return null;

    final List<GwtModule> gwtModules = gwtModulesManager.findGwtModulesByClientSourceFile(virtualFile);
    if (gwtModules.isEmpty()) return null;

    final List<ProblemDescriptor> problemDescriptors = new SmartList<>();
    file.accept(new JavaRecursiveElementWalkingVisitor() {
      @Override
      public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
        final ProcessingContext context = new ProcessingContext();
        if (INCORRECT_CREATE_METHOD_CALL_PATTERN.accepts(expression, context)) {
          final PsiExpression argument = context.get(GWT_CREATE_ARGUMENT_KEY);
          final TextRange argumentTextRange = argument.getTextRange();
          if (argumentTextRange.getStartOffset() >= argumentTextRange.getEndOffset()) {
            return;
          }
          final String message = GwtBundle.message("problem.description.only.class.literals.may.be.used.as.arguments.to.gwt.create");
          problemDescriptors.add(manager.createProblemDescriptor(argument, message, isOnTheFly, LocalQuickFix.EMPTY_ARRAY,
                                                                 ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
        }
      }
    });
    return !problemDescriptors.isEmpty() ? problemDescriptors.toArray(ProblemDescriptor.EMPTY_ARRAY) : null;
  }
}
