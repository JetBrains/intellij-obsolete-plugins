package com.intellij.lang.puppet.formatter;

import com.intellij.formatting.Wrap;
import com.intellij.formatting.WrapType;
import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.PuppetTokenTypes;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class PuppetWrappingProcessor {
  private static final Wrap NORMAL_WRAP = Wrap.createWrap(WrapType.NORMAL, false);
  private static final Wrap NONE_WRAP = Wrap.createWrap(WrapType.NONE, false);

  private final @NotNull PsiFile myFile;

  /**
   * Returns map of the here-doc openers per line
   */
  private final NotNullLazyValue<List<TextRange>> myHeredocsRangesList;

  PuppetWrappingProcessor(@NotNull PsiFile file) {
    myFile = file;
    myHeredocsRangesList = NotNullLazyValue.atomicLazy(() -> {
          final Document document = myFile.getViewProvider().getDocument();
          if (document == null) {
            return Collections.emptyList();
          }

          List<TextRange> result = new ArrayList<>();
          myFile.accept(new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
              super.visitElement(element);
              if (PsiUtilCore.getElementType(element) == PuppetTokenTypes.HEREDOC_EXPRESSION) {
                int startOffset = element.getNode().getStartOffset();
                result.add(TextRange.create(startOffset + 1, document.getLineEndOffset(document.getLineNumber(startOffset))));
              }
              else {
                element.acceptChildren(this);
              }
            }
          });

          return result;
        });
  }

  /**
   * Checks if there is a here-doc opener on the line before this node and, therefore, we can't wrap the line
   *
   * @param node node in question
   * @return true if there is
   */
  public boolean isHereDocOnLineBefore(@NotNull ASTNode node) {
    int startOffset = node.getStartOffset();
    for (TextRange range : myHeredocsRangesList.getValue()) {
      if (range.contains(startOffset)) {
        return true;
      }
    }

    return false;
  }

  public @Nullable Wrap getNodeWrap(final @NotNull ASTNode node) {
    PsiElement psiElement = node.getPsi();
    if (psiElement instanceof PsiFile) {
      return null;
    }

    return isHereDocOnLineBefore(node) ? NONE_WRAP : NORMAL_WRAP;
  }
}
