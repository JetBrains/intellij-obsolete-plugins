// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.codeInsight.navigation.NavigationGutterIconRenderer;
import com.intellij.guice.GuiceBundle;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.util.NotNullFactory;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.util.ConstantFunction;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Collection;
import java.util.List;

/**
 * The gutter icon builder of the Guice line markers. It differs from {@link NavigationGutterIconBuilder} in
 * one point: {@link #createGuiceLineMarkerInfo} creates a {@link GuiceLineMarkerInfo}, a marker class that
 * belongs to the plugin class loader.
 *
 * <p>On a dynamic unload the platform removes the highlighters of the plugin from the open editors
 * ({@code DaemonListeners.removeHighlightersOnPluginUnload}) and recognises a line marker by the class loader
 * of its {@link com.intellij.codeInsight.daemon.LineMarkerInfo} class. A plain {@link RelatedItemLineMarkerInfo}
 * is a platform class: such a marker stays in the gutter until the next highlighting pass, and its renderer
 * keeps the plugin's target-renderer lambda, and with it the plugin class loader, alive.
 */
final class GuiceGutterIconBuilder extends NavigationGutterIconBuilder<PsiElement> {

  GuiceGutterIconBuilder(@NotNull Icon icon) {
    super(icon, DEFAULT_PSI_CONVERTOR, element -> List.of(new GotoRelatedItem(element, GuiceBundle.GUICE)));
  }

  /**
   * Like {@link #createLineMarkerInfo(PsiElement)}, with a marker class of the plugin.
   */
  @NotNull RelatedItemLineMarkerInfo<PsiElement> createGuiceLineMarkerInfo(@NotNull PsiElement element) {
    NavigationGutterIconRenderer renderer = createGutterIconRenderer(element.getProject(), null);
    GutterIconNavigationHandler<PsiElement> navigationHandler = renderer.isNavigateAction() ? renderer : null;
    String tooltip = renderer.getTooltipText();
    return new GuiceLineMarkerInfo(
        element,
        element.getTextRange(),
        renderer.getIcon(),
        tooltip == null ? null : new ConstantFunction<>(tooltip),
        navigationHandler,
        renderer.getAlignment(),
        this::computeGotoTargets);
  }

  /**
   * A {@link RelatedItemLineMarkerInfo} whose class the plugin class loader owns, so that the platform removes
   * the marker from the open editors when the plugin unloads. It adds no behaviour.
   */
  static final class GuiceLineMarkerInfo extends RelatedItemLineMarkerInfo<PsiElement> {
    GuiceLineMarkerInfo(@NotNull PsiElement element,
                        @NotNull TextRange range,
                        Icon icon,
                        @Nullable Function<? super PsiElement, String> tooltipProvider,
                        @Nullable GutterIconNavigationHandler<PsiElement> navHandler,
                        @NotNull GutterIconRenderer.Alignment alignment,
                        @NotNull NotNullFactory<? extends Collection<? extends GotoRelatedItem>> targets) {
      super(element, range, icon, tooltipProvider, navHandler, alignment, targets);
    }
  }
}
