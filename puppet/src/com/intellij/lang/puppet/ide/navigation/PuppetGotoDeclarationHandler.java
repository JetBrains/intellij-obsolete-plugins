package com.intellij.lang.puppet.ide.navigation;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.lang.puppet.PuppetTokenTypes;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PuppetGotoDeclarationHandler implements GotoDeclarationHandler, PuppetTokenTypes {
  private static final TokenSet ACCEPTABLE_ELEMENTS = TokenSet.create(
    VARIABLE_NAME,
    NAME,
    CAPITALIZED_NAME,
    SINGLE_QUOTED_STRING
  );

  @Override
  public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement sourceElement, int offset, Editor editor) {

    if (!ACCEPTABLE_ELEMENTS.contains(PsiUtilCore.getElementType(sourceElement))) {
      return null;
    }

    List<PsiElement> result = new ArrayList<>();
    int offsetInElement = offset - sourceElement.getNode().getStartOffset();
    PsiElement referencesProvider = sourceElement.getParent();
    for (PsiReference reference : referencesProvider.getReferences()) {
      if (reference.getRangeInElement().contains(offsetInElement)) {
        if (reference instanceof PsiPolyVariantReference) {
          for (ResolveResult resolveResult : ((PsiPolyVariantReference)reference).multiResolve(false)) {
            PsiElement targetElement = resolveResult.getElement();
            if (targetElement != null) {
              result.add(targetElement);
            }
          }
        }
        else {
          PsiElement targetElement = reference.resolve();
          if (targetElement != null) {
            result.add(targetElement);
          }
        }
      }
    }
    return result.toArray(PsiElement.EMPTY_ARRAY);
  }
}
