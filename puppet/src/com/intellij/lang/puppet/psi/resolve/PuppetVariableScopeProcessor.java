package com.intellij.lang.puppet.psi.resolve;

import com.intellij.psi.PsiFile;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public abstract class PuppetVariableScopeProcessor implements PsiScopeProcessor, PuppetNamedPsiElementProcessor {
  private final Set<PsiFile> processedFiles = new HashSet<>();

  public boolean processFile(@NotNull PsiFile psiFile) {
    return processedFiles.add(psiFile);
  }

  public Set<PsiFile> getProcessedFiles() {
    return processedFiles;
  }
}
