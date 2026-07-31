package com.intellij.gwt.i18n;

import com.intellij.lang.properties.codeInspection.unused.ImplicitPropertyUsageProvider;
import com.intellij.lang.properties.psi.Property;
import org.jetbrains.annotations.NotNull;

/**
 * Marks a property as implicitly used when it backs a method of a GWT i18n {@code Constants}/{@code Messages}
 * interface. Such properties are referenced through the generated interface method rather than by their key, so the
 * platform "Unused property" inspection would otherwise report them as unused (IDEA-109938, IDEA-118654).
 */
public final class GwtImplicitPropertyUsageProvider implements ImplicitPropertyUsageProvider {
  @Override
  public boolean isUsed(@NotNull Property property) {
    return GwtI18nManager.getInstance(property.getProject()).getMethod(property) != null;
  }
}
