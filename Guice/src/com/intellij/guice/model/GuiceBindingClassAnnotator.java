// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.GuiceIcons;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.beans.BindToProviderDescriptor;
import com.intellij.guice.model.renderers.GuiceBindingClassPsiElementListCellRenderer;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.util.NotNullFunction;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class GuiceBindingClassAnnotator extends RelatedItemLineMarkerProvider {

  @Override
  protected void collectNavigationMarkers(@NotNull PsiElement psiElement, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {

    if (psiElement instanceof PsiClass) {
      final Set<BindDescriptor> descriptors =
        GuiceInjectorManager.getBindingDescriptors(ModuleUtilCore.findModuleForPsiElement(psiElement));

      Set<BindDescriptor> bindingDescriptors = new HashSet<>();

      for (BindDescriptor descriptor : descriptors) {
        final PsiClass boundClass = descriptor.getBoundClass();
        if (psiElement.equals(boundClass)) {
          bindingDescriptors.add(descriptor);
          continue;
        }

        if (psiElement.equals(getBindingBaseClass(descriptor.getBindingClass()))) {
          bindingDescriptors.add(descriptor);
          continue;
        }

        if (descriptor instanceof BindToProviderDescriptor) {
          final PsiClass providerClass = ((BindToProviderDescriptor)descriptor).getProviderClass();
          if (psiElement.equals(getBindingBaseClass(providerClass))) bindingDescriptors.add(descriptor);
        }
      }

      if (!bindingDescriptors.isEmpty()) {
        final NavigationGutterIconBuilder<BindDescriptor> builder =
          NavigationGutterIconBuilder.create(GuiceIcons.GoogleSmall, DEFAULT_CONVERTOR).
            setPopupTitle(GuiceBundle.message("GuiceClassAnnotator.popup.title")).
            setTooltipText(GuiceBundle.message("GuiceClassAnnotator.popup.tooltip.text")).
            setCellRenderer(GuiceBindingClassPsiElementListCellRenderer::new).
            setTargets(bindingDescriptors);

        final PsiIdentifier identifier = ((PsiClass)psiElement).getNameIdentifier();
        if (identifier != null) {
          result.add(builder.createLineMarkerInfo(identifier));
        }
      }
    }
  }

  private static @Nullable PsiClass getBindingBaseClass(@Nullable PsiClass bindingClass) {
    if (bindingClass instanceof PsiAnonymousClass) {
      return ((PsiAnonymousClass)bindingClass).getBaseClassType().resolve();
    }
    return bindingClass;
  }

  public static final NotNullFunction<BindDescriptor, Collection<? extends PsiElement>> DEFAULT_CONVERTOR =
    o -> ContainerUtil.createMaybeSingletonList(o.getBindExpression());
}
