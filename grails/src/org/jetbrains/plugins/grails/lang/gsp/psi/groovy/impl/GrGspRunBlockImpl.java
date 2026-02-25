// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.groovy.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.TokenType;
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.lexer.core.GspTokenTypes;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GrGspDeclarationHolder;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GrGspExprInjection;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GrGspRunBlock;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GrGspRunMethod;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GspGroovyFile;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspFile;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspScriptletTag;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspGrailsTag;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyTokenTypes;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.blocks.GrOpenBlockImpl;

public class GrGspRunBlockImpl extends GrOpenBlockImpl implements GrGspRunBlock {
  private static final String GSPBLOCK_SYNTHETIC_NAME = "GspRunBlock";

  public GrGspRunBlockImpl(@NotNull IElementType type, CharSequence buffer) {
    super(type, buffer);
  }

  @Override
  public String toString() {
    return GSPBLOCK_SYNTHETIC_NAME;
  }

  @Override
  public GrGspDeclarationHolder[] getDeclarationHolders() {
    return findChildrenByClass(GrGspDeclarationHolder.class);
  }

  @Override
  public @NotNull GrStatement addStatementBefore(@NotNull GrStatement element, @Nullable GrStatement anchor) throws IncorrectOperationException {
    if (anchor != null && !this.equals(anchor.getParent())) {
      throw new IncorrectOperationException();
    }

    ASTNode elemNode = element.copy().getNode();
    assert elemNode != null;
    if (anchor instanceof GrGspExprInjection) {
      GspFile gspFile = ((GspGroovyFile) getContainingFile()).getGspLanguageRoot();
      PsiElement injectionStart = anchor.getPrevSibling();
      assert injectionStart != null;
      PsiElement elem = gspFile.findElementAt(injectionStart.getNode().getStartOffset());
      assert elem != null;
      // groovy expression injection holder
      PsiElement parent = elem.getParent();

      ASTNode treePrev = anchor.getNode().getTreePrev();
      treePrev = findAppropriateScriptletEndElement(treePrev, parent);

      ASTNode blockNode = getNode();
      boolean createNewTag = treePrev == null;
      if (createNewTag) {
        treePrev = blockNode.getFirstChildNode();

        blockNode.addLeaf(GspTokenTypes.JSCRIPT_BEGIN, "<%", treePrev);
        blockNode.addLeaf(GroovyTokenTypes.mNLS, "\n", treePrev);
        blockNode.addLeaf(TokenType.WHITE_SPACE, " ", treePrev);
        elemNode = CodeEditUtil.addChildren(getNode(), elemNode, elemNode, treePrev);
        blockNode.addLeaf(GroovyTokenTypes.mNLS, "\n", treePrev);
        blockNode.addLeaf(GspTokenTypes.JSCRIPT_END, "%>\n", treePrev);
      }
      else {
        ASTNode openTag = treePrev.getTreePrev();
        while (openTag != null &&
            openTag.getPsi() instanceof PsiWhiteSpace) {
          openTag = openTag.getTreePrev();
        }
        if (openTag != null &&
            GroovyTokenTypes.mNLS != openTag.getElementType()) {
          blockNode.addLeaf(GroovyTokenTypes.mNLS, "\n", treePrev);
        }
        blockNode.addLeaf(TokenType.WHITE_SPACE, " ", treePrev);
        elemNode = CodeEditUtil.addChildren(getNode(), elemNode, elemNode, treePrev);
        blockNode.addLeaf(GroovyTokenTypes.mNLS, "\n", treePrev);
      }
      ASTNode fileNode = gspFile.getNode();
      assert fileNode != null;
      return (GrStatement) elemNode.getPsi();
    } else {
      return super.addStatementBefore(element, anchor);
    }
  }

  private ASTNode findAppropriateScriptletEndElement(ASTNode treePrev, PsiElement exprParent) {
    while (treePrev != null &&
        !(GspTokenTypes.GSCRIPT_END == treePrev.getElementType() ||
            GspTokenTypes.JSCRIPT_END == treePrev.getElementType())) {
      treePrev = treePrev.getTreePrev();
    }
    GspFile gspFile = ((GspGroovyFile) getContainingFile()).getGspLanguageRoot();
    if (treePrev == null) return treePrev;
    PsiElement elem = gspFile.findElementAt(treePrev.getStartOffset());
    if (elem != null &&
        elem.getParent() instanceof GspScriptletTag) {
      PsiElement prevParent = elem.getParent();
      if (prevParent.getParent() instanceof GspGrailsTag &&
          exprParent != null &&
          exprParent.getParent() != prevParent.getParent()) {
        treePrev = treePrev.getTreePrev();
        return findAppropriateScriptletEndElement(treePrev, exprParent);
      }
    }
    return treePrev;
  }

  @Override
  public boolean isTopControlFlowOwner() {
    return getParent() instanceof GrGspRunMethod;
  }
}
