package com.intellij.lang.puppet.ide.usages;

import com.intellij.ide.util.DeleteNameDescriptionLocation;
import com.intellij.ide.util.DeleteTypeDescriptionLocation;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.psi.PuppetFullQualifiedNameOwner;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.ElementDescriptionLocation;
import com.intellij.psi.ElementDescriptionProvider;
import com.intellij.psi.ElementDescriptionUtil;
import com.intellij.psi.PsiElement;
import com.intellij.usageView.UsageViewLongNameLocation;
import com.intellij.usageView.UsageViewShortNameLocation;
import com.intellij.usageView.UsageViewTypeLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;

final class PuppetElementDescriptionProvider implements ElementDescriptionProvider {
  @Override
  public @Nullable String getElementDescription(@NotNull PsiElement element, @NotNull ElementDescriptionLocation location) {
    if (!element.getLanguage().isKindOf(PuppetLanguage.INSTANCE)) {
      return null;
    }

    if (location == DeleteNameDescriptionLocation.INSTANCE) {
      return ElementDescriptionUtil.getElementDescription(element, UsageViewShortNameLocation.INSTANCE);
    }
    else if (location == DeleteTypeDescriptionLocation.SINGULAR)
    {
      return ElementDescriptionUtil.getElementDescription(element, UsageViewTypeLocation.INSTANCE);
    }
    else if( location == DeleteTypeDescriptionLocation.PLURAL)
    {
      return StringUtil.pluralize(ElementDescriptionUtil.getElementDescription(element, DeleteTypeDescriptionLocation.SINGULAR));
    }
    else if( location == UsageViewShortNameLocation.INSTANCE && element instanceof PuppetFullQualifiedNameOwner)
    {
      return ((PuppetFullQualifiedNameOwner)element).getFullQualifiedName();
    }
    else if (location == UsageViewLongNameLocation.INSTANCE) {
      return MessageFormat.format(
        "{0} ''{1}''",
        ElementDescriptionUtil.getElementDescription(element, UsageViewTypeLocation.INSTANCE),
        ElementDescriptionUtil.getElementDescription(element, UsageViewShortNameLocation.INSTANCE)
      );
    }
    return null;
  }
}
