package com.intellij.lang.puppet.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface PuppetFqnContainer extends PuppetCompositePsiElement {

  @NotNull
  PsiElement getNameIdentifier();

  @Nullable
  String getNamespaceName();

  @Nullable
  String getContainingNamespaceNameForElement(@NotNull PsiElement element);

  @Nullable
  String getFullQualifiedName();

  @NotNull
  List<PsiPuppetNamespaceDefinition> getNamespaceDefinitionList();
}
