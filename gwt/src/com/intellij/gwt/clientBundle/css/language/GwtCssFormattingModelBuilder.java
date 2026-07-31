package com.intellij.gwt.clientBundle.css.language;

import com.intellij.formatting.Block;
import com.intellij.formatting.ChildAttributes;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Spacing;
import com.intellij.gwt.clientBundle.css.language.psi.GwtCssExternal;
import com.intellij.gwt.clientBundle.css.language.psi.impl.GwtCssDeclarationElementBase;
import com.intellij.lang.ASTNode;
import com.intellij.lang.css.CSSLanguage;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.css.CssElementVisitor;
import com.intellij.psi.css.CssRuleset;
import com.intellij.psi.css.CssRulesetList;
import com.intellij.psi.css.codeStyle.CssCodeStyleSettings;
import com.intellij.psi.css.impl.CssRulesetWrappingElement;
import com.intellij.psi.css.impl.util.editor.CssFormattingModelBuilder;
import com.intellij.psi.formatter.FormatterUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class GwtCssFormattingModelBuilder extends CssFormattingModelBuilder {
  @Override
  protected @NotNull CssFormattingExtension createExtension(@NotNull CodeStyleSettings settings) {
    return new CssFormattingExtension(settings.getCommonSettings(CSSLanguage.INSTANCE),
                                      settings.getCustomSettings(CssCodeStyleSettings.class)) {
      @Override
      public boolean addSubBlocks(PsiElement element, List<Block> result) {
        final ASTNode node = element.getNode();
        if (element instanceof GwtCssDeclarationElementBase || element instanceof GwtCssExternal) {
          result.add(new CssSimpleBlock(node, Indent.getNoneIndent(), this));
          return true;
        }
        if (element instanceof CssRulesetWrappingElement) {
          result.add(new RulesetWrapperElementBlock(node, Indent.getNoneIndent(), this));
          return true;
        }
        return false;
      }
    };
  }

  private static final class RulesetWrapperElementBlock extends CssFormatterBlock {

    private ArrayList<Block> mySubBlocks;

    private RulesetWrapperElementBlock(ASTNode _node, Indent indent, CssFormattingExtension extension) {
      super(_node, indent, extension);
    }

    @Override
    public @NotNull List<Block> getSubBlocks() {
      if (mySubBlocks == null) {
        mySubBlocks = new ArrayList<>();
        myNode.getPsi().acceptChildren(new CssElementVisitor() {
          @Override
          public void visitCssRuleset(CssRuleset ruleset) {
            mySubBlocks.add(new CssRulesetBlock(ruleset.getNode(), Indent.getNormalIndent(), myExtension, null));
          }

          @Override
          public void visitCssRulesetList(CssRulesetList rulesetList) {
            rulesetList.acceptChildren(this);
          }

          @Override
          public void visitElement(@NotNull PsiElement element) {
            final ASTNode node = element.getNode();
            if (!FormatterUtil.containsWhiteSpacesOnly(node)) {
              mySubBlocks.add(new LeafBlock(node, Indent.getNoneIndent()));
            }
          }
        });
      }

      return mySubBlocks;
    }

    @Override
    public Spacing getSpacing(Block child1, @NotNull Block child2) {
      return null;
    }

    @Override
    public @NotNull ChildAttributes getChildAttributes(int newChildIndex) {
      return new ChildAttributes(Indent.getNormalIndent(), null);
    }
  }
}
