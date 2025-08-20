package com.intellij.play.language.formatter;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.ChildAttributes;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Wrap;
import com.intellij.formatting.templateLanguages.DataLanguageBlockWrapper;
import com.intellij.formatting.templateLanguages.TemplateLanguageBlock;
import com.intellij.formatting.templateLanguages.TemplateLanguageBlockFactory;
import com.intellij.lang.ASTNode;
import com.intellij.play.language.PlayElementTypes;
import com.intellij.play.language.psi.PlayTag;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PlayBlock extends TemplateLanguageBlock {

  public PlayBlock(ASTNode node,
                   Wrap wrap,
                   Alignment alignment,
                   CodeStyleSettings settings,
                   @NotNull TemplateLanguageBlockFactory blockFactory,
                   @Nullable List<DataLanguageBlockWrapper> foreignChildren) {
    super(node, wrap, alignment, blockFactory, settings, foreignChildren);
  }

  @Override
  protected IElementType getTemplateTextElementType() {
    return PlayElementTypes.TEMPLATE_TEXT;
  }

  @NotNull
  @Override
  public ChildAttributes getChildAttributes(int newChildIndex) {
    return new ChildAttributes(Indent.getNoneIndent(), null);
  }

  @Override
  public Indent getIndent() {
    if (getParent() == null || getParent().getParent() == null) return Indent.getNoneIndent();
    final PsiElement psi = getNode().getPsi();
    if (psi instanceof PlayTag) {
      if (psi.getParent() instanceof PsiFile) return Indent.getNoneIndent();

      return Indent.getNormalIndent();
    }
    return Indent.getNoneIndent();
  }
}
