package com.intellij.play.language;

import com.intellij.lang.ASTNode;
import com.intellij.play.utils.PlayUtils;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.GroovyElementVisitor;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement;

public class PlayCompositeGroovyExpressionElement extends PlayCompositeElement implements GroovyPsiElement {
  public PlayCompositeGroovyExpressionElement(ASTNode node) {
    super(node);
  }

  @Override
  public boolean processDeclarations(@NotNull PsiScopeProcessor processor,
                                     @NotNull ResolveState state,
                                     PsiElement lastParent,
                                     @NotNull PsiElement place) {
    if (PlayUtils.processPlayDeclarations(processor, state, this)) return false;

    return super.processDeclarations(processor, state, lastParent, place);
  }

  @Override
  public void accept(@NotNull GroovyElementVisitor visitor) {
  }

  @Override
  public void acceptChildren(@NotNull GroovyElementVisitor visitor) {
  }
}
