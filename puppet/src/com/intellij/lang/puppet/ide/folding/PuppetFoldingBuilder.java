package com.intellij.lang.puppet.ide.folding;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.CustomFoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.PuppetTokenTypes;
import com.intellij.lang.puppet.psi.PuppetResourceInstanceDeclaration;
import com.intellij.lang.puppet.psi.PuppetResourceLikeClassDescription;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PuppetFoldingBuilder extends CustomFoldingBuilder implements PuppetTokenTypes {
  private static final TokenSet FOLDABLE_ELEMENTS_WITH_QUOTES = TokenSet.create(
    BRACED_STATEMENTS_BLOCK,
    PARENTHESIZED_PARAMETERS_LIST_BLOCK,
    BRACED_SELECTOR_VALUES_BLOCK,
    BRACED_CASE_OPTS_BLOCK
  );

  private static final NotNullLazyValue<Map<IElementType, String>> REGION_NAMES_LAZY_MAP = NotNullLazyValue.atomicLazy(() -> {
    Map<IElementType, String> result = new HashMap<>();
    result.put(PARENTHESIZED_PARAMETERS_LIST_BLOCK, PuppetBundle.message("puppet.folding.arguments"));
    result.put(BRACED_STATEMENTS_BLOCK, PuppetBundle.message("puppet.folding.code"));
    result.put(HEREDOC_BODY, PuppetBundle.message("puppet.folding.heredoc"));
    result.put(HEREDOC_BODY_QQ, PuppetBundle.message("puppet.folding.heredoc"));
    result.put(RESOURCE_INSTANCE_DECLARATION, PuppetBundle.message("puppet.folding.attributes"));
    result.put(RESOURCE_LIKE_CLASS_DESCRIPTION, PuppetBundle.message("puppet.folding.attributes"));
    result.put(BRACED_SELECTOR_VALUES_BLOCK, PuppetBundle.message("puppet.folding.selector.options"));
    result.put(BRACED_CASE_OPTS_BLOCK, PuppetBundle.message("puppet.folding.case.options"));
    return result;
  });

  @Override
  protected void buildLanguageFoldRegions(@NotNull List<FoldingDescriptor> descriptors,
                                          @NotNull PsiElement root,
                                          @NotNull Document document,
                                          boolean quick) {
    root.accept(new PsiElementVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        IElementType elementType = PsiUtilCore.getElementType(element);
        if (FOLDABLE_ELEMENTS_WITH_QUOTES.contains(elementType)) {
          TextRange elementRange = element.getTextRange();
          if (elementRange.getLength() > 2) {
            descriptors
              .add(new FoldingDescriptor(element, TextRange.create(elementRange.getStartOffset() + 1, elementRange.getEndOffset() - 1)));
          }
        }
        else if (elementType == HEREDOC_BODY || elementType == HEREDOC_BODY_QQ) {
          TextRange elementRange = element.getTextRange();
          if (elementRange.getLength() > 1) {
            descriptors
              .add(new FoldingDescriptor(element, TextRange.create(elementRange.getStartOffset(), elementRange.getEndOffset() - 1)));
          }
        }
        else if (element instanceof PuppetResourceInstanceDeclaration || element instanceof PuppetResourceLikeClassDescription) {
          FoldingDescriptor descriptor = getResourceInstanceLikeFoldingDescriptor(element);
          if (descriptor != null) {
            descriptors.add(descriptor);
          }
        }

        super.visitElement(element);
        element.acceptChildren(this);
      }
    });
  }

  private static @Nullable FoldingDescriptor getResourceInstanceLikeFoldingDescriptor(PsiElement element) {
    ASTNode elementNode = element.getNode();
    TextRange nodeRange = elementNode.getTextRange();
    int elementEndOffset = nodeRange.getEndOffset();
    ASTNode colonElement = elementNode.findChildByType(COLON);
    if (colonElement == null) {
      return null;
    }

    int colonOffset = colonElement.getStartOffset();

    return colonOffset < elementEndOffset ? new FoldingDescriptor(element, TextRange.create(colonOffset, elementEndOffset)) : null;
  }

  @Override
  protected String getLanguagePlaceholderText(@NotNull ASTNode node, @NotNull TextRange range) {
    IElementType elementType = PsiUtilCore.getElementType(node);
    String explicitName = REGION_NAMES_LAZY_MAP.getValue().get(elementType);
    return explicitName == null ? "..." : explicitName;
  }

  @Override
  protected boolean isRegionCollapsedByDefault(@NotNull ASTNode node) {
    return false;
  }
}
