package com.intellij.lang.puppet.ide.refactoring;

import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiNamedElement;
import com.intellij.refactoring.rename.inplace.MemberInplaceRenameHandler;
import com.intellij.refactoring.rename.inplace.MemberInplaceRenamer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PuppetMemberInplaceRenameHandler extends MemberInplaceRenameHandler {
  @Override
  protected boolean isAvailable(@Nullable PsiElement element, @NotNull Editor editor, @NotNull PsiFile file) {
    return element != null && element.getLanguage() == PuppetLanguage.INSTANCE && element instanceof PsiNamedElement;
  }

  @Override
  protected @NotNull MemberInplaceRenamer createMemberRenamer(@NotNull PsiElement element,
                                                              @NotNull PsiNameIdentifierOwner elementToRename,
                                                              @NotNull Editor editor) {
    return new PuppetMemberInplaceRenamer(elementToRename, element, editor);
  }
}
