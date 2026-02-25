// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.formatter;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.Block;
import com.intellij.formatting.ChildAttributes;
import com.intellij.formatting.FormattingContext;
import com.intellij.formatting.FormattingModel;
import com.intellij.formatting.FormattingModelBuilder;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.Wrap;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageFormatting;
import com.intellij.lang.javascript.JSTokenTypes;
import com.intellij.lang.javascript.JavaScriptSupportLoader;
import com.intellij.lang.javascript.JavascriptLanguage;
import com.intellij.lang.javascript.formatter.JSBlockContext;
import com.intellij.lang.javascript.formatter.JSFormatterUtil;
import com.intellij.lang.javascript.formatter.JavascriptFormattingModelBuilder;
import com.intellij.lang.javascript.formatter.blocks.JSBlock;
import com.intellij.lang.javascript.formatter.blocks.JSDocCommentBlock;
import com.intellij.lang.javascript.formatter.blocks.SubBlockVisitor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.css.CssStylesheet;
import com.intellij.psi.formatter.xml.XmlFormattingPolicy;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.addins.GrailsIntegrationUtil;
import org.jetbrains.plugins.grails.addins.js.CssIntegrationUtil;
import org.jetbrains.plugins.grails.addins.js.JavaScriptIntegrationUtil;
import org.jetbrains.plugins.grails.lang.gsp.formatter.processors.GspIndentProcessor;

import java.util.List;

public final class GspBlockGenerator {
  private GspBlockGenerator() {
  }

  /**
   * Creates child block for block with given child by it's textRange
   *
   * @param child     node of parent block
   * @param textRange textRange of child block
   */
  public static void createGspBlockByTextRange(List<? super Block> result,
                                               ASTNode child,
                                               Wrap wrap,
                                               Alignment alignment,
                                               XmlFormattingPolicy policy,
                                               TextRange textRange) {

    ASTNode parent = child.getTreeParent();

    Indent indent = Indent.getNoneIndent();
    if (parent != null) {
      PsiElement parentPsi = parent.getPsi();
      if (parentPsi instanceof XmlTag && policy.indentChildrenOf((XmlTag)parentPsi)) {
        indent = Indent.getNormalIndent();
      }
    }

    if (!child.getTextRange().contains(textRange)) return;

    String text = child.getPsi().getContainingFile().getText();
    int start = textRange.getStartOffset();
    int end = textRange.getEndOffset();
    String s = end < text.length() - 1 ?
            text.substring(start, end) :
            text.substring(start);
    if (s.trim().isEmpty()) return;

    while (!text.substring(start, start + 1).equals(text.substring(start, start + 1).trim())) {
      start++;
    }
    while (!text.substring(end - 1, end).equals(text.substring(end - 1, end).trim())) {
      end--;
    }

    assert start < end;

    result.add(new GspBlock(child, wrap, alignment, policy, indent, new TextRange(start, end)));
  }

  /**
   * Creates block by child ASTNode
   *
   * @param child given childNode
   */
  public static void createGspBlockByChildNode(List<? super Block> result,
                                               ASTNode parentNode,
                                               ASTNode child,
                                               Wrap wrap,
                                               Alignment alignment,
                                               XmlFormattingPolicy policy) {
    if (!canBeCorrectBlock(child)) {
      return;
    }
    result.add(new GspBlock(child, wrap, alignment, policy, GspIndentProcessor.getGspChildIndent(parentNode, child, policy), child.getTextRange()));
  }

  /**
   * Creates block by child ASTNode
   *
   * @param child given childNode
   */
  public static void createHtmlBlockByChildNode(List<Block> result,
                                                ASTNode parentNode,
                                                ASTNode child,
                                                Wrap wrap,
                                                Alignment alignment,
                                                XmlFormattingPolicy policy,
                                                XmlTag[] nestedGspTags) {
    if (!canBeCorrectBlock(child)) {
      return;
    }

    PsiElement childPsi = child.getPsi();
    if (GrailsIntegrationUtil.isJsSupportEnabled() &&
            (JavaScriptIntegrationUtil.isJavaScriptInjection(childPsi) ||
                    JavaScriptIntegrationUtil.isJSEmbeddedContent(childPsi))) {
      createForeignLanguageBlock(JavaScriptSupportLoader.JAVASCRIPT.getLanguage(),
              child, result, policy, policy.getSettings());
    } else if (GrailsIntegrationUtil.isCssSupportEnabled() && childPsi instanceof CssStylesheet) {
      createForeignLanguageBlock(childPsi.getLanguage(),
              child, result, policy, policy.getSettings());
    } else {
      result.add(new GspHtmlBlock(child,
              wrap,
              alignment,
              policy,
              GspIndentProcessor.getGspChildIndent(parentNode, child, policy),
              nestedGspTags));
    }
  }


  public static void createForeignLanguageBlock(final Language childLanguage,
                                                final ASTNode child,
                                                final List<Block> result, final XmlFormattingPolicy policy,
                                                final CodeStyleSettings settings) {
    final PsiElement childPsi = child.getPsi();
    FormattingModelBuilder builder;
    if (JavaScriptIntegrationUtil.isJavaScriptInjection(childPsi)) {
      generateBlockForJSInjection(childPsi, result, settings);
    } else {
      builder = LanguageFormatting.INSTANCE.forContext(childLanguage, childPsi);
      if (builder != null) {
        final FormattingModel childModel = builder.createModel(FormattingContext.create(childPsi, settings));
        final Indent childIndent = GrailsIntegrationUtil.isCssSupportEnabled() && CssIntegrationUtil.isCssLanguage(childLanguage)
                                   ? Indent.getNoneIndent()
                                   : Indent.getNormalIndent();
        result.add(new ForeignLanguageBlock(child,
                policy,
                childModel.getRootBlock(), childIndent));
      }
    }
  }

  private static void generateBlockForJSInjection(PsiElement outer, List<Block> result, CodeStyleSettings settings) {
    PsiFile file = outer.getContainingFile();
    final int offset = outer.getTextRange().getStartOffset();
    PsiElement element = InjectedLanguageUtil.findElementAtNoCommit(file, offset);
    FormattingModelBuilder builder = getJSFormattingModelBuilder(offset, file, outer);
    final FormattingModel childModel = builder.createModel(FormattingContext.create(element, settings));
    Block rootJsBlock = childModel.getRootBlock();
    result.add(rootJsBlock);
  }


  private static FormattingModelBuilder getJSFormattingModelBuilder(final int offset, final PsiFile file, final PsiElement outer) {
    return new FormattingModelBuilder() {
      @Override
      public @NotNull FormattingModel createModel(@NotNull FormattingContext formattingContext) {
        CodeStyleSettings settings = formattingContext.getCodeStyleSettings();
        JSBlock jsBlock = new MyJSRootBlock(outer, settings, offset, file);
        return JavascriptFormattingModelBuilder.createJSFormattingModel(file, settings, jsBlock);
      }
    };
  }

  public static boolean canBeCorrectBlock(final ASTNode node) {
    return node != null && (!node.getText().trim().isEmpty());
  }

  private static class MyJSSubBlockVisitor extends SubBlockVisitor {
    private final CodeStyleSettings mySettings;
    private final int myOffset;

    MyJSSubBlockVisitor(CodeStyleSettings settings, int offset) {
      super(null, null, JSBlockContext.createDefault(settings));
      mySettings = settings;
      myOffset = offset;
    }

    @Override
    protected void addBlock(final Block block) {
      if (block instanceof JSDocCommentBlock) {
        super.addBlock(new Block() {
          @Override
          public @NotNull TextRange getTextRange() {
            return block.getTextRange().shiftRight(myOffset);
          }

          @Override
          public @NotNull List<Block> getSubBlocks() {
            return block.getSubBlocks();
          }

          @Override
          public Wrap getWrap() {
            return block.getWrap();
          }

          @Override
          public Indent getIndent() {
            return block.getIndent();
          }

          @Override
          public Alignment getAlignment() {
            return block.getAlignment();
          }

          @Override
          public Spacing getSpacing(Block child1, @NotNull Block child2) {
            return block.getSpacing(child1, child2);
          }

          @Override
          public @NotNull ChildAttributes getChildAttributes(int newChildIndex) {
            return block.getChildAttributes(newChildIndex);
          }

          @Override
          public boolean isIncomplete() {
            return block.isIncomplete();
          }

          @Override
          public boolean isLeaf() {
            return block.isLeaf();
          }
        });
      }
      else {
        super.addBlock(block);
      }
    }

    @Override
    public void visitElement(final ASTNode node) {
      Wrap sharedWrap =
        JSFormatterUtil.createSharedWrapForChildren(node, mySettings.getCommonSettings(JavascriptLanguage.INSTANCE), myCustomSettings);
      ASTNode child = node.getFirstChildNode();
      while (child != null) {
        if (child.getElementType() != JSTokenTypes.WHITE_SPACE &&
                child.getTextRange().getLength() > 0) {
          Wrap wrap = getWrap(node, child, sharedWrap);
          Indent childIndent = getIndent(node, child, null);
          JSBlock jsBlock = new MyJSBlock(child, null, childIndent, wrap, mySettings, myOffset);
          getBlocks().add(jsBlock);
        }
        child = child.getTreeNext();
      }
    }
  }

  private static class MyJSBlock extends JSBlock {
    private final int myOffset;
    private final CodeStyleSettings mySettings;

    MyJSBlock(ASTNode child, Alignment childAlignment, Indent childIndent, Wrap wrap, CodeStyleSettings settings, int offset) {
      super(child, childAlignment, childIndent, wrap, settings);
      myOffset = offset;
      mySettings = settings;
    }

    @Override
    public @NotNull TextRange getTextRange() {
      // Shifted text range
      return super.getTextRange().shiftRight(myOffset);
    }

    @Override
    protected SubBlockVisitor createSubBlockVisitor() {
      return new MyJSSubBlockVisitor(mySettings, myOffset);
    }
  }

  private static final class MyJSRootBlock extends JSBlock {

    private List<Block> mySubBlocks;
    private final CodeStyleSettings mySettings;
    private final int myOffset;
    private final PsiFile myFile;

    private MyJSRootBlock(PsiElement outer, CodeStyleSettings settings, int offset, PsiFile file) {
      super(outer.getNode(), null, Indent.getNormalIndent(), null, settings);
      mySettings = settings;
      myOffset = offset;
      myFile = file;
      mySubBlocks = null;
    }

    @Override
    public @NotNull List<Block> getSubBlocks() {
      if (mySubBlocks == null) {
        SubBlockVisitor visitor = new MyJSSubBlockVisitor(mySettings, myOffset);
        PsiFile jsFile = InjectedLanguageUtil.findInjectedPsiNoCommit(myFile, myOffset);
        if (jsFile != null && jsFile.getNode() != null) {
          visitor.visit(jsFile.getNode());
        }
        mySubBlocks = visitor.getBlocks();
      }
      return mySubBlocks;
    }
  }
}
