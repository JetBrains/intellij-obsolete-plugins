package com.intellij.lang.puppet.ide.usages;

import com.intellij.lang.HelpID;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.lang.puppet.psi.PuppetCompositePsiElement;
import com.intellij.lang.puppet.psi.PuppetFullQualifiedNameOwner;
import com.intellij.lang.puppet.psi.PuppetLazyProxyLightElement;
import com.intellij.lang.puppet.util.PuppetTypesInfoUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PuppetFindUsagesProvider implements FindUsagesProvider {

  @Override
  public boolean canFindUsagesFor(@NotNull PsiElement psiElement) {
    return psiElement instanceof PuppetCompositePsiElement && psiElement instanceof PsiNamedElement;
  }

  @Override
  public @Nullable String getHelpId(@NotNull PsiElement psiElement) {
    return HelpID.FIND_OTHER_USAGES;
  }

  @Override
  public @NotNull String getType(@NotNull PsiElement element) {
    String typeName = PuppetTypesInfoUtil.getTypeName(element);

    return StringUtil.notNullize(typeName);
  }

  @Override
  public @NotNull String getDescriptiveName(@NotNull PsiElement element) {
    return StringUtil.notNullize(((PsiNamedElement)element).getName());
  }

  @Override
  public @NotNull String getNodeText(@NotNull PsiElement element, boolean useFullName) {
    if (element instanceof PuppetLazyProxyLightElement) {
      return ((PuppetLazyProxyLightElement)element).getName();
    }
    if (element instanceof PuppetFullQualifiedNameOwner) {
      String name = ((PuppetFullQualifiedNameOwner)element).getFullQualifiedName();
      if (name != null) {
        return name;
      }
    }
    if (element instanceof PsiNamedElement) {
      String name = ((PsiNamedElement)element).getName();
      if (name != null) {
        return name;
      }
    }
    return element.getText();
  }
}
