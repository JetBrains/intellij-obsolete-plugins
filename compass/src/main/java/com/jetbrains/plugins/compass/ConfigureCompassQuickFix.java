package com.jetbrains.plugins.compass;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.QuestionAction;
import com.intellij.codeInspection.HintAction;
import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

public class ConfigureCompassQuickFix extends LocalQuickFixOnPsiElement implements HintAction {
  private final Module myModule;

  public ConfigureCompassQuickFix(@NotNull Module module, @NotNull PsiElement element) {
    super(element);
    myModule = module;
  }

  @NotNull
  @Override
  public String getText() {
    return "Configure Compass";
  }

  @Override
  public void invoke(@NotNull final Project project,
                     @NotNull PsiFile file,
                     @NotNull PsiElement startElement,
                     @NotNull PsiElement endElement) {
    invokeAction(project);
  }

  @Override
  public void invoke(@NotNull final Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    invokeAction(project);
  }

  @NotNull
  @Override
  public String getFamilyName() {
    return getText();
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    return true;
  }

  private void invokeAction(@NotNull final Project project) {
    ApplicationManager.getApplication().invokeLater(() ->
            ShowSettingsUtil.getInstance().editConfigurable(project, CompassUtil.createCompassConfigurable(myModule, false)));
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  @Override
  public boolean showHint(@NotNull final Editor editor) {
    if (HintManager.getInstance().hasShownHintsThatWillHideByOtherHint(true)) {
      return false;
    }
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return false;
    }

    final PsiElement element = getStartElement();
    if (myModule.isDisposed() || element == null || !element.isValid()) {
      return false;
    }

    String message = getText() + "? " + KeymapUtil.getFirstKeyboardShortcutText(
      ActionManager.getInstance().getAction(IdeActions.ACTION_SHOW_INTENTION_ACTIONS)
    );

    HintManager.getInstance().showQuestionHint(
      editor,
      message,
      element.getTextRange().getStartOffset(),
      element.getTextRange().getEndOffset(),
      new QuestionAction() {
        @Override
        public boolean execute() {
          invokeAction(element.getProject());
          return true;
        }
      }
    );
    return true;
  }
}
