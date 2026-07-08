package com.intellij.lang.puppet.formatter;

import com.intellij.formatting.Alignment;
import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.psi.PsiPuppetBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static com.intellij.lang.puppet.PuppetTokenTypes.ARGUMENT;
import static com.intellij.lang.puppet.PuppetTokenTypes.DEFAULT_SELECTOR_VALUE;
import static com.intellij.lang.puppet.PuppetTokenTypes.EQUALS;
import static com.intellij.lang.puppet.PuppetTokenTypes.FARROW;
import static com.intellij.lang.puppet.PuppetTokenTypes.HASH_ARGUMENT;
import static com.intellij.lang.puppet.PuppetTokenTypes.HASH_PAIR;
import static com.intellij.lang.puppet.PuppetTokenTypes.HASH_VALUE;
import static com.intellij.lang.puppet.PuppetTokenTypes.PARAMETER;
import static com.intellij.lang.puppet.PuppetTokenTypes.PARENTHESIZED_PARAMETERS_LIST_BLOCK;
import static com.intellij.lang.puppet.PuppetTokenTypes.PARROW;
import static com.intellij.lang.puppet.PuppetTokenTypes.RESOURCE_ARGUMENTS_LIST;
import static com.intellij.lang.puppet.PuppetTokenTypes.SELECTOR_VALUE;

class PuppetAlignmentProcessor {
  private static final TokenSet ARROWS_NON_BLOCK_GRANDPARENTS = TokenSet.create(
    HASH_VALUE,
    RESOURCE_ARGUMENTS_LIST
  );

  private static final TokenSet ARROWS_PARENTS = TokenSet.create(
    ARGUMENT, HASH_ARGUMENT, SELECTOR_VALUE, DEFAULT_SELECTOR_VALUE, HASH_PAIR
  );
  private static final TokenSet ARROWS_TOKENS = TokenSet.create(
    FARROW, PARROW
  );

  private final Map<ASTNode, Alignment> myAlignmentMap = new HashMap<>();

  public @Nullable Alignment getNodeAlignment(final @NotNull ASTNode node) {
    final ASTNode parentNode = node.getTreeParent();
    if (parentNode == null) {
      return null;
    }
    ASTNode grandParentNode = parentNode.getTreeParent();
    PsiElement grandParentElement = grandParentNode == null ? null : grandParentNode.getPsi();
    IElementType grandParentElementType = PsiUtilCore.getElementType(grandParentNode);
    IElementType parentElementType = PsiUtilCore.getElementType(parentNode);
    IElementType elementType = PsiUtilCore.getElementType(node);

    if (grandParentNode == null) {
      return null;
    }

    if (ARROWS_TOKENS.contains(elementType) && ARROWS_PARENTS.contains(parentElementType) &&
        (grandParentElement instanceof PsiPuppetBlock || ARROWS_NON_BLOCK_GRANDPARENTS.contains(grandParentElementType))
      ) {
      return getAlignmentBase(grandParentNode);
    }
    else if (elementType == EQUALS && parentElementType == PARAMETER && grandParentElementType == PARENTHESIZED_PARAMETERS_LIST_BLOCK) {
      return getAlignmentBase(grandParentNode);
    }

    return null;
  }

  @SuppressWarnings("unused")
  public @Nullable Alignment getNewChildAlignment(ASTNode parent, ASTNode prevChild) {
    return null;
  }

  private @NotNull Alignment getAlignmentBase(@NotNull ASTNode node) {
    Alignment result = myAlignmentMap.get(node);
    if (result == null) {
      result = Alignment.createAlignment(true, Alignment.Anchor.LEFT);
      myAlignmentMap.put(node, result);
    }
    return result;
  }
}
