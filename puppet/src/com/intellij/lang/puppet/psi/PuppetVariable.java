package com.intellij.lang.puppet.psi;

import com.intellij.lang.puppet.PuppetTokenTypes;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import org.jetbrains.annotations.Nullable;

public interface PuppetVariable extends PuppetCompositePsiElement,
                                        PsiNameIdentifierOwner,
                                        NavigatablePsiElement,
                                        PuppetFullQualifiedNameOwner,
                                        PuppetTokenTypes {

  boolean isDeclaration();

  boolean isMetaparameter();

  boolean isParameter();

  boolean isCoreFact();

  boolean isBuiltIn();

  /**
   * @return true if variable specified with fqn
   */
  boolean isFullQualified();

  /**
   * @return true if variable unavailable with fqn from outside of the current lexical scope
   */
  boolean isLexicalDeclaration();

  @Nullable
  PuppetScopeHolder getScopeHolder();
}
