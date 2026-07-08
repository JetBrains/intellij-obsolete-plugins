package com.intellij.lang.puppet.formatter;

import com.intellij.formatting.Indent;
import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.PuppetTokenTypes;
import com.intellij.lang.puppet.psi.PsiPuppetBlock;
import com.intellij.lang.puppet.psi.PsiPuppetExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class PuppetIndentProcessor implements PuppetTokenTypes {
  static final Indent DELEGATE_TO_PREV = Indent.getSpaceIndent(-239);

  private static final TokenSet BLOCK_LIKE_TOKENS = TokenSet.create(
    ARRAY,
    HASH_VALUE,
    EXPRESSION_PAREN,
    COLLECT_R_HAND,
    EXPRESSION_INDEX,
    HASH_ARRAY_INDEX
  );

  private static final TokenSet CONTINUATION_WITHOUT_FIRST_INDENTED_CONTAINERS = TokenSet.create(
    ARGUMENT,
    SELECTOR,

    RESOURCE_INSTANCE_DECLARATION,
    RESOURCE_LIKE_CLASS_DESCRIPTION
  );

  private static final TokenSet INDENTABLE_EXPRESSIONS_CONTAINERS = TokenSet.create(
    QUOTED_TEXT,
    HASH_ARRAY_ACCESSES,
    HASH_ARRAY_ACCESS
  );

  private static final TokenSet UNINDENTABLE_ELEMENTS = TokenSet.create(
    BRACED_CASE_OPTS_BLOCK
  );

  private static final TokenSet UNINDENTABLE_CONTAINERS = TokenSet.create(
    IF_STATEMENT,
    UNLESS_STATEMENT,
    EXPRESSION_RELATION
  );

  private static final TokenSet ABSOLUTE_UNINDENTABLE_ELEMENTS = TokenSet.create(
    HEREDOC_BODY,
    HEREDOC_BODY_QQ,
    HEREDOC_ENDING
  );

  public @Nullable Indent getNodeIndent(final @NotNull ASTNode node) {
    IElementType nodeElementType = PsiUtilCore.getElementType(node);
    if (UNINDENTABLE_ELEMENTS.contains(nodeElementType)) {
      return Indent.getNoneIndent();
    }
    else if (ABSOLUTE_UNINDENTABLE_ELEMENTS.contains(nodeElementType)) {
      return Indent.getAbsoluteNoneIndent();
    }


    ASTNode parentNode = node.getTreeParent();
    if (parentNode == null) {
      return null;
    }

    PsiElement parentElement = parentNode.getPsi();
    IElementType parentElementType = PsiUtilCore.getElementType(parentNode);


    if (parentElement instanceof PsiPuppetBlock || BLOCK_LIKE_TOKENS.contains(parentElementType)) {
      if (node.getTreeNext() != null && node.getTreePrev() != null) {
        return Indent.getNormalIndent();
      }
    }
    else if (UNINDENTABLE_CONTAINERS.contains(parentElementType)) {
      return Indent.getNoneIndent();
    }
    else if (CONTINUATION_WITHOUT_FIRST_INDENTED_CONTAINERS.contains(parentElementType) || parentElement instanceof PsiPuppetExpression) {
      return Indent.getContinuationWithoutFirstIndent();
    }
    else if (INDENTABLE_EXPRESSIONS_CONTAINERS.contains(parentElementType) && node.getPsi() instanceof PsiPuppetExpression) {
      return Indent.getNormalIndent();
    }

    return Indent.getNoneIndent();
  }

  public @NotNull Indent getChildNodeIndent(@NotNull ASTNode parentNode, @Nullable ASTNode prevChildNode) {
    IElementType parentElementType = PsiUtilCore.getElementType(parentNode);
    PsiElement parentElement = parentNode.getPsi();
    IElementType prevChildElementType = PsiUtilCore.getElementType(prevChildNode);

    if (parentElementType == BRACED_RESOURCE_BY_CLASSNAME_CONTENTS_BLOCK && prevChildElementType == RESOURCE_INSTANCE_DECLARATION ||
        parentElementType == RESOURCE_LIKE_CLASS_DECLARATION_BLOCK && prevChildElementType == RESOURCE_LIKE_CLASS_DESCRIPTION
    ) {
      return DELEGATE_TO_PREV;
    }
    if (BLOCK_LIKE_TOKENS.contains(parentElementType) || parentElement instanceof PsiPuppetBlock) {
      return Indent.getNormalIndent();
    }
    else if (parentElementType == RESOURCE_INSTANCE_DECLARATION || parentElementType == RESOURCE_LIKE_CLASS_DESCRIPTION) {
      return Indent.getContinuationIndent();
    }

    return Indent.getNoneIndent();
  }
}
