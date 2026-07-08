package com.intellij.lang.puppet.psi.mixins;

import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.psi.PuppetPsiUtil;
import com.intellij.lang.puppet.psi.PuppetResourceLikeClassDescription;
import com.intellij.lang.puppet.psi.impl.PuppetCompositePsiElementBase;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.lang.puppet.util.PuppetQualifiedNamesUtil.SEPARATOR;

public class PuppetResourceLikeClassDescriptionMixin extends PuppetCompositePsiElementBase implements PuppetResourceLikeClassDescription {
  public PuppetResourceLikeClassDescriptionMixin(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull List<PsiElement> getNameIdentifiersList() {
    return PuppetPsiUtil.getResourceLikeIdentifiersList(getFirstChild());
  }

  @Override
  public String getNameFromIdentifier(PsiElement nameIdentifier) {
    return PuppetPsiUtil.getResourceLikeNameFromIdentifier(nameIdentifier);
  }

  @Override
  public @NotNull List<String> getNamesList() {
    return ContainerUtil.map(PuppetPsiUtil.computeResourceLikeNamesList(this), bareName -> StringUtil.trimStart(bareName, SEPARATOR));
  }
}
