// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.providers;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.helidon.HelidonIcons;
import com.intellij.helidon.constants.HelidonConstants;
import com.intellij.helidon.utils.HelidonBundle;
import com.intellij.helidon.utils.HelidonCommonUtils;
import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.*;
import com.intellij.psi.presentation.java.SymbolPresentationUtil;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.uast.UastSmartPointer;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;

public final class HelidonClassAnnotator extends RelatedItemLineMarkerProvider {

  private static DefaultPsiElementCellRenderer getMethodCallRendered() {
    return new DefaultPsiElementCellRenderer() {
      @Override
      protected Icon getIcon(PsiElement element) {
        return HelidonIcons.HelidonGutter;
      }

      @Override
      public String getContainerText(PsiElement element, String name) {
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        if (psiClass != null) {
          return SymbolPresentationUtil.getSymbolPresentableText(psiClass);
        }
        return SymbolPresentationUtil.getSymbolContainerText(element);
      }
    };
  }

  @Override
  public void collectNavigationMarkers(@NotNull List<? extends PsiElement> elements,
                                       @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result,
                                       boolean forNavigation) {
    final PsiElement psiElement = ContainerUtil.getFirstItem(elements);
    if (psiElement == null) return;
    Module module = ModuleUtilCore.findModuleForPsiElement(psiElement);
    if (HelidonCommonUtils.hasHelidonLibrary(module)) {
      for (PsiElement element : elements) {
        collectNavigationMarkers(element, module, result);
      }
    }
  }

  private static void collectNavigationMarkers(@NotNull PsiElement psiElement,
                                               @NotNull Module module,
                                               @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    if (psiElement instanceof PsiIdentifier) {
      final PsiElement parent = psiElement.getParent();
      if (parent instanceof PsiClass) {
        if (InheritanceUtil.isInheritor((PsiClass)parent, HelidonConstants.SERVICE)) {
          Set<UExpression> calls =
            getServiceRegisterExpressions(module, JavaPsiFacade.getInstance(module.getProject()).getElementFactory()
              .createType((PsiClass)parent));

          Set<PsiElement> targets =
            calls.stream().map(expression -> expression.getSourcePsi()).filter(Objects::nonNull).collect(Collectors.toSet());
          if (!targets.isEmpty()) {
            NavigationGutterIconBuilder<PsiElement> builder =
              NavigationGutterIconBuilder.create(HelidonIcons.HelidonGutter, HelidonBundle.HELIDON_LIBRARY).
                setTargets(targets).
                setPopupTitle(HelidonBundle.message("gutter.choose.service.registration")).
                setTooltipText(HelidonBundle.message("gutter.navigate.to.service.registration")).
                setCellRenderer(HelidonClassAnnotator::getMethodCallRendered);
            result.add(builder.createLineMarkerInfo(psiElement));
          }
        }
      }
    }
  }

  private static @NotNull Set<UExpression> getServiceRegisterExpressions(@NotNull Module module, @NotNull PsiClassType serviceType) {
    Set<UExpression> expressions = new HashSet<>();
    for (UCallExpression call : getServiceRegistrationCalls(module, serviceType)) {
      List<UExpression> arguments = call.getValueArguments();
      if (arguments.size() == 2) {
        PsiType expressionType = arguments.get(1).getExpressionType();
        if (expressionType != null && serviceType.isAssignableFrom(expressionType)) {
          ContainerUtil.addIfNotNull(expressions, arguments.get(0));
        }
      }
    }
    return expressions;
  }

  private static @NotNull Set<UCallExpression> getServiceRegistrationCalls(@NotNull Module module, @NotNull PsiType type) {
    Set<UCallExpression> calls = new HashSet<>();
    for (Map.Entry<UastSmartPointer<UCallExpression>, PsiType> entry : HelidonCommonUtils.getServiceRegisterInvocations(module)
      .entrySet()) {
      if (entry.getValue().isAssignableFrom(type)) ContainerUtil.addIfNotNull(calls, entry.getKey().getElement());
    }
    return calls;
  }

  @Override
  public @NonNls String getName() {
    return "Helidon Services";
  }

  @Override
  public String getId() {
    return "HelidonClassAnnotator";
  }

  @Override
  public Icon getIcon() {
    return HelidonIcons.Helidon;
  }
}
