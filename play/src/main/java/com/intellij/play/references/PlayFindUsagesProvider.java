package com.intellij.play.references;

import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.play.language.psi.PlayPsiFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class PlayFindUsagesProvider implements FindUsagesProvider {

    @Override
    public boolean canFindUsagesFor(@NotNull final PsiElement psiElement) {
        return psiElement instanceof PlayPsiFile; // custom tags references
    }

    @Override
    public String getHelpId(@NotNull final PsiElement psiElement) {
        return null;
    }

    @Override
    @NotNull @NonNls
    public String getType(@NotNull final PsiElement element) {
      if (element instanceof PsiFile) return "file";
      return "";
    }

    @Override
    @NotNull
    public String getDescriptiveName(@NotNull final PsiElement element) {
      if (element instanceof PsiFile) return ((PsiFile)element).getName();

      return "";
    }

    @Override
    @NotNull
    public String getNodeText(@NotNull final PsiElement element, final boolean useFullName) {
        return element.getText();
    }
}

