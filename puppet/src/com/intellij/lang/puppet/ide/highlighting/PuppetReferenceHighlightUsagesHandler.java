package com.intellij.lang.puppet.ide.highlighting;

import com.intellij.lang.puppet.psi.references.PuppetPolyVariantCachingReferenceBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.ResolveResult;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Highlighting puppet usages starting from reference
 */
public class PuppetReferenceHighlightUsagesHandler extends PuppetHighlightUsagesHandlerBase {
  private final PuppetPolyVariantCachingReferenceBase myReference;

  public PuppetReferenceHighlightUsagesHandler(@NotNull Editor editor,
                                               @NotNull PsiFile file,
                                               @NotNull PuppetPolyVariantCachingReferenceBase reference) {
    super(editor, file);
    myReference = reference;
  }

  @Override
  public @NotNull List<PsiElement> getTargets() {
    Set<PsiElement> referenceTargets = new HashSet<>();
    for (ResolveResult resolveResult : myReference.multiResolve(false)) {
      PsiElement element = resolveResult.getElement();
      if (element != null) {
        referenceTargets.add(element);
      }
    }

    return getTargetsWithSynonyms(myReference.getElement().getContainingFile(), referenceTargets);
  }
}
