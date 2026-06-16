// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.GuiceIcons;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.renderers.GuiceBindingClassTargetPresentationRenderer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.util.NotNullFunction;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UastContextKt;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class GuiceBindingClassAnnotator extends RelatedItemLineMarkerProvider {

  @Override
  public void collectNavigationMarkers(@NotNull List<? extends PsiElement> elements,
                                       @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result,
                                       boolean forNavigation) {
    if (elements.isEmpty()) return;
    Module module = ModuleUtilCore.findModuleForPsiElement(elements.getFirst());
    if (module == null) return;
    GuiceProjectModel model = GuiceProjectModel.getInstance(module.getProject());
    if (!model.isGuiceAvailable(module)) return;
    super.collectNavigationMarkers(elements, result, forNavigation);
  }

  @Override
  protected void collectNavigationMarkers(@NotNull PsiElement psiElement, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    final PsiElement parent = psiElement.getParent();
    if (!(parent instanceof PsiNameIdentifierOwner) || !psiElement.equals(((PsiNameIdentifierOwner)parent).getNameIdentifier())) {
      return;
    }

    final UElement uElement = UastContextKt.toUElement(parent);
    if (!(uElement instanceof UClass uClass)) return;
    final PsiClass psiClass = uClass.getJavaPsi();

    final Module module = ModuleUtilCore.findModuleForPsiElement(psiElement);
    if (module == null) return;

    GuiceProjectModel model = GuiceProjectModel.getInstance(module.getProject());
    if (!model.isGuiceAvailable(module)) return;

    GuiceLiveIndex index = model.getIndex(module);
    Set<BindDescriptor> bindingDescriptors = index.findBindingsForClass(psiClass);

    if (!bindingDescriptors.isEmpty()) {
      final NavigationGutterIconBuilder<BindDescriptor> builder =
        NavigationGutterIconBuilder.create(GuiceIcons.GoogleSmall, DEFAULT_CONVERTOR).
          setPopupTitle(GuiceBundle.message("GuiceClassAnnotator.popup.title")).
          setTooltipText(GuiceBundle.message("GuiceClassAnnotator.popup.tooltip.text")).
          setTargetRenderer(GuiceBindingClassTargetPresentationRenderer::new).
          setTargets(bindingDescriptors);

      result.add(NonPersistentLineMarkerInfo.createFrom(builder, psiElement));
    }
  }

  public static final NotNullFunction<BindDescriptor, Collection<? extends PsiElement>> DEFAULT_CONVERTOR =
    o -> ContainerUtil.createMaybeSingletonList(o.getBindExpression());
}
