package com.intellij.plugins.jboss.arquillian.inspection;

import com.intellij.codeInsight.daemon.QuickFixBundle;
import com.intellij.codeInsight.daemon.impl.quickfix.AddMethodFix;
import com.intellij.codeInsight.intention.AddAnnotationFix;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateEditingAdapter;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.ConstantNode;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.jboss.arquillian.ArquillianBundle;
import com.intellij.plugins.jboss.arquillian.utils.ArquillianUtils;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.intellij.plugins.jboss.arquillian.constants.ArquillianConstants.DEPLOYMENT_CLASS;
import static com.intellij.plugins.jboss.arquillian.constants.ArquillianConstants.JAVA_ARCHIVE_CLASS;

public class ArquillianDeploymentAbsentInspection extends ArquillianDeploymentCountInspectionBase {

  @NonNls static final String deploymentMethodName = "createDeployment";

  private static void addProblemIfAnchorNotNull(@NotNull ProblemsHolder holder,
                                                @Nullable PsiElement anchor,
                                                LocalQuickFix @NotNull [] fixes) {
    if (anchor == null) {
      return;
    }
    holder.registerProblem(holder.getManager().createProblemDescriptor(
      anchor,
      ArquillianBundle.message("arquillian.deployment.absent"),
      holder.isOnTheFly(),
      fixes,
      ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
  }

  private static LocalQuickFix createCreateDeploymentMethodFix(final PsiClass aClass, Set<String> conflictNames) {
    int index = 0;
    String methodName = deploymentMethodName;
    while (conflictNames.contains(methodName)) {
      methodName = deploymentMethodName + ++index;
    }
    FileTemplate fileTemplate = FileTemplateManager.getInstance(aClass.getProject()).getCodeTemplate("Arquillian Deployment Method.java");
    String templateText = fileTemplate.getText();
    String deploymentRegex = "\\$\\{DEPLOYMENT_NAME\\}";
    String methodText = templateText.replaceAll(deploymentRegex, methodName);
    PsiMethod newMethod = JavaPsiFacade
      .getInstance(aClass.getProject())
      .getElementFactory()
      .createMethodFromText(methodText, aClass);
    return new MyAddMethodFix(newMethod, aClass, methodName);
  }

  @Override
  protected void checkDeploymentMethods(@NotNull PsiClass aClass,
                                        @NotNull List<PsiMethod> deploymentMethods,
                                        @NotNull ProblemsHolder holder) {
    List<LocalQuickFix> fixList = new ArrayList<>();
    PsiType shrinkWrapArchiveType = PsiType.getTypeByName(
      JAVA_ARCHIVE_CLASS,
      aClass.getProject(),
      GlobalSearchScope.allScope(aClass.getProject()));
    Set<String> conflictNames = new HashSet<>();

    for (PsiMethod method : aClass.getMethods()) {
      if (method.getParameterList().getParametersCount() > 0) {
        continue;
      }
      PsiModifierList modifierList = method.getModifierList();
      if (!modifierList.hasModifierProperty(PsiModifier.STATIC)) {
        continue;
      }
      conflictNames.add(method.getName());
      PsiType methodReturnType = method.getReturnType();
      if (methodReturnType == null || !TypeConversionUtil.isAssignable(shrinkWrapArchiveType, methodReturnType)) {
        continue;
      }
      AddAnnotationFix addAnnotationFix = new AddAnnotationFix(DEPLOYMENT_CLASS, method);
      fixList.add(addAnnotationFix);
      PsiIdentifier nameIdentifier = method.getNameIdentifier();
      holder.registerProblem(
        nameIdentifier == null ? method : nameIdentifier,
        ArquillianBundle.message("arquillian.deployment.candidate"),
        ProblemHighlightType.WEAK_WARNING,
        addAnnotationFix);
    }
    fixList.add(0, createCreateDeploymentMethodFix(aClass, conflictNames));
    LocalQuickFix[] fixes = fixList.toArray(LocalQuickFix.EMPTY_ARRAY);
    addProblemIfAnchorNotNull(holder, ArquillianUtils.getJunitArquillianEnabledElement(aClass), fixes);
    addProblemIfAnchorNotNull(holder, ArquillianUtils.getTestngArquillianEnabledElement(aClass), fixes);
  }

  @Override
  protected boolean wouldLikeToCheckMethodCount(int count) {
    return count == 0;
  }

  private static class MyAddMethodFix extends LocalQuickFixAndIntentionActionOnPsiElement {
    private final @NotNull AddMethodFix myFix;
    private final String myMethodName;
    @IntentionName private final String myText;

    private MyAddMethodFix(PsiMethod newMethod, PsiClass aClass, String methodName) {
      super(aClass);
      myFix = new AddMethodFix(newMethod, aClass);
      myMethodName = methodName;
      myText = QuickFixBundle.message("add.method.text", myFix, aClass.getName());
    }

    @Override
    public @NotNull String getFamilyName() {
      return myFix.getFamilyName();
    }

    @Override
    public @NotNull String getText() {
      return myText;
    }

    @Override
    public void invoke(@NotNull Project project,
                       @NotNull PsiFile file,
                       @Nullable Editor editor,
                       @NotNull PsiElement startElement,
                       @NotNull PsiElement endElement) {
      PsiMethod newMethod = myFix.createMethod((PsiClass)startElement);
      final PsiIdentifier nameIdentifier = newMethod.getNameIdentifier();
      if (editor == null || nameIdentifier == null) {
        return;
      }

      PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());
      final TextRange range = nameIdentifier.getTextRange();
      editor.getDocument().replaceString(range.getStartOffset(), range.getEndOffset(), "");
      editor.getCaretModel().moveToOffset(nameIdentifier.getTextRange().getStartOffset());
      Template template = TemplateManager.getInstance(project).createTemplate("", "");
      Expression nameExpr = new ConstantNode(myMethodName);
      template.addVariable(myMethodName, nameExpr, nameExpr, true);

      TemplateManager.getInstance(project).startTemplate(editor, template, new TemplateEditingAdapter() {
      });
    }

    @Override
    public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
      // Unsupported
      return IntentionPreviewInfo.EMPTY;
    }
  }
}
