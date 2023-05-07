package com.intellij.play.utils.processors;

import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;

public interface PlayDeclarationsProcessor {
  /**
   * @return false to stop processing.
   */
  boolean processElement(PsiScopeProcessor processor, ResolveState state, final PsiElement scope);
}
