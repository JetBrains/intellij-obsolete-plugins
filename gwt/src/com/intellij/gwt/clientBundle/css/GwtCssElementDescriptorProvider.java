package com.intellij.gwt.clientBundle.css;

import com.intellij.gwt.clientBundle.css.language.psi.impl.GwtCssFileImpl;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.css.CssPropertyDescriptor;
import com.intellij.psi.css.StylesheetFile;
import com.intellij.psi.css.descriptor.CssContextType;
import com.intellij.psi.css.descriptor.CssPropertyDescriptorStub;
import com.intellij.psi.css.impl.util.scheme.CssElementDescriptorProviderImpl;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class GwtCssElementDescriptorProvider extends CssElementDescriptorProviderImpl {
  public static final @NonNls String GWT_IMAGE_PROPERTY_NAME = "gwt-image";

  @Override
  public boolean isMyContext(@Nullable PsiElement context) {
    if (context == null || !context.isValid()) return false;
    PsiFile psiFile = InjectedLanguageManager.getInstance(context.getProject()).getTopLevelFile(context);
    if (psiFile == null) return false;
    final VirtualFile file = psiFile.getOriginalFile().getVirtualFile();
    return GwtFacet.findFacetBySourceFile(context.getProject(), file) != null;
  }

  @Override
  public @NotNull Collection<? extends CssPropertyDescriptor> getAllPropertyDescriptors(@Nullable PsiElement context) {
    Collection<CssPropertyDescriptor> result = new ArrayList<>();
    result.add(new CssPropertyDescriptorStub(GWT_IMAGE_PROPERTY_NAME));
    result.addAll(super.getAllPropertyDescriptors(context));
    return result;
  }

  @Override
  public CssPropertyDescriptor getPropertyDescriptor(@NotNull String propertyName, @Nullable PsiElement context) {
    if (GWT_IMAGE_PROPERTY_NAME.equals(propertyName)) {
      return new GwtPropertyDescriptor(GWT_IMAGE_PROPERTY_NAME);
    }
    return super.getPropertyDescriptor(propertyName, context);
  }

  @Override
  public @NotNull Collection<? extends CssPropertyDescriptor> findPropertyDescriptors(@NotNull String propertyName, @Nullable PsiElement context) {
    if (GWT_IMAGE_PROPERTY_NAME.equals(propertyName)) {
      return List.of(new GwtPropertyDescriptor(GWT_IMAGE_PROPERTY_NAME));
    }
    return super.findPropertyDescriptors(propertyName, context);
  }

  @Override
  public boolean shouldAskOtherProviders(@Nullable PsiElement context) {
    PsiFile file = context != null ? context.getContainingFile() : null;
    return file instanceof StylesheetFile && !(file instanceof GwtCssFileImpl);
  }

  @Override
  public CssContextType getCssContextType(@Nullable PsiElement context) {
    return CssContextType.ANY;
  }
}
