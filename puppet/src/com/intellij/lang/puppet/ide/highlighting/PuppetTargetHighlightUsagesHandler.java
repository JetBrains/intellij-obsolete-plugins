package com.intellij.lang.puppet.ide.highlighting;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Highlighting puppet usages starting from one of declarations
 */
public class PuppetTargetHighlightUsagesHandler extends PuppetHighlightUsagesHandlerBase {
  private final @NotNull PsiElement myNamedElement;

  public PuppetTargetHighlightUsagesHandler(@NotNull Editor editor, @NotNull PsiFile file, @NotNull PsiElement namedElement) {
    super(editor, file);
    myNamedElement = namedElement;
  }

  @Override
  public @NotNull List<PsiElement> getTargets() {
    PsiFile file = myNamedElement.getContainingFile();
    assert file != null;
    return getTargetsWithSynonyms(file, Collections.singletonList(myNamedElement));
  }
}
