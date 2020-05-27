package com.intellij.lang.javascript.linter.jscs;

import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInsight.daemon.impl.actions.SuppressByCommentFix;
import com.intellij.codeInspection.SuppressQuickFix;
import com.intellij.lang.javascript.formatter.JSCodeStyleSettings;
import com.intellij.lang.javascript.inspections.JSInspectionSuppressor;
import com.intellij.lang.javascript.linter.JSLinterInspection;
import com.intellij.lang.javascript.linter.JSLinterUtil;
import com.intellij.lang.javascript.psi.JSElement;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Irina.Chernushina on 9/22/2014.
 */
public class JscsInspection extends JSLinterInspection {

  @NotNull
  @Override
  protected JscsExternalAnnotator getExternalAnnotatorForBatchInspection() {
    return JscsExternalAnnotator.getInstanceForBatchInspection();
  }

  @Override
  public SuppressQuickFix @NotNull [] getBatchSuppressActions(@Nullable PsiElement element) {
    return new SuppressQuickFix[] {
      new JscsSuppressForFileByCommentFix(HighlightDisplayKey.find(getShortName()), JSInspectionSuppressor.getHolderClass(element)),
      new JscsSuppressForLineByCommentFix(HighlightDisplayKey.find(getShortName()), JSInspectionSuppressor.getHolderClass(element))};
  }

  public static class JscsSuppressForLineByCommentFix extends SuppressByCommentFix {
    public JscsSuppressForLineByCommentFix(@NotNull HighlightDisplayKey key,
                                           @NotNull Class<? extends PsiElement> suppressionHolderClass) {
      super(key, suppressionHolderClass);
    }

    @NotNull
    @Override
    public String getText() {
      return "Suppress all JSCS rules for current line";
    }

    @Override
    protected void createSuppression(@NotNull Project project, @NotNull PsiElement element, @NotNull PsiElement container)
      throws IncorrectOperationException {
      final Document document = JSLinterUtil.getDocumentForElement(element);
      if (document == null) return;
      final int lineNo = document.getLineNumber(element.getTextOffset());

      suppressRuleForLine(project, document, element.getContainingFile(), lineNo, element.getTextOffset(), "");
    }

    public static void suppressRuleForLine(final Project project,
                                           final Document document,
                                           final PsiFile file,
                                           final int line,
                                           final int elementOffset,
                                           final String rule) {
      CommandProcessor.getInstance().executeCommand(project, () -> {
        final int lineEndOffset = document.getLineEndOffset(line);
        PsiElement contextElement = file.findElementAt(elementOffset);
        String commentPrefix = contextElement instanceof JSElement ? JSCodeStyleSettings.getLineCommentPrefix(file) : " ";
        if (!lineHasComments(document, file, line, elementOffset)) {
          document.insertString(lineEndOffset, String.format("//%sjscs:ignore %s", commentPrefix, rule));
        } else {
          final int lineStartOffset = document.getLineStartOffset(line);
          document.insertString(lineEndOffset, String.format("\n//%sjscs:enable %s", commentPrefix, rule));
          document.insertString(lineStartOffset, String.format("//%sjscs:disable %s\n", commentPrefix, rule));
        }
      }, null, null);
    }

    private static boolean lineHasComments(final Document document, PsiFile file, final int line, final int elementOffset) {
      final boolean[] result = new boolean[1];
      final int endOffset = document.getLineEndOffset(line);
      PsiTreeUtil.processElements(file, new PsiElementProcessor() {
        @Override
        public boolean execute(@NotNull PsiElement element) {
          final TextRange range = element.getTextRange();
          if (element instanceof PsiComment && range.getStartOffset() > elementOffset && range.getStartOffset() < endOffset) {
            result[0] = true;
            return false;
          }
          return true;
        }
      });
      return result[0];
    }
  }

  private static class JscsSuppressForFileByCommentFix extends SuppressByCommentFix {
    JscsSuppressForFileByCommentFix(@NotNull HighlightDisplayKey key,
                                           @NotNull Class<? extends PsiElement> suppressionHolderClass) {
      super(key, suppressionHolderClass);
    }

    @NotNull
    @Override
    public String getText() {
      return "Suppress all JSCS rules for file";
    }

    @Override
    protected void createSuppression(@NotNull Project project, @NotNull PsiElement element, @NotNull PsiElement container)
      throws IncorrectOperationException {
      final Document document = JSLinterUtil.getDocumentForElement(element);
      if (document == null) return;
      CommandProcessor.getInstance().executeCommand(project, () -> document.insertString(0, String
        .format("//%sjscs:disable\n", JSCodeStyleSettings.getLineCommentPrefix(container))), null, null);
    }
  }
}
