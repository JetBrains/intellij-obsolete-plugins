// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.codeInsight.navigation.NavigationGutterIconRenderer;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.util.NotNullFactory;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Collection;

/**
 * A {@link RelatedItemLineMarkerInfo} subclass that merges multiple Guice gutter icons
 * on the same line into a single action chooser popup.
 */
final class NonPersistentLineMarkerInfo<T extends PsiElement> extends RelatedItemLineMarkerInfo<T> {

  private NonPersistentLineMarkerInfo(@NotNull T element,
                                      @NotNull TextRange range,
                                      Icon icon,
                                      @Nullable Function<? super T, String> tooltipProvider,
                                      @Nullable GutterIconNavigationHandler<T> navHandler,
                                      @NotNull GutterIconRenderer.Alignment alignment,
                                      @NotNull NotNullFactory<? extends Collection<? extends GotoRelatedItem>> targets) {
    super(element, range, icon, tooltipProvider, navHandler, alignment, targets);
  }

  /**
   * Creates a {@link RelatedItemLineMarkerInfo} from a {@link NavigationGutterIconBuilder},
   * mirroring {@link NavigationGutterIconBuilder#createLineMarkerInfo(PsiElement)}.
   *
   * @param builder the builder configured with icon, targets, tooltip, etc.
   * @param element the PSI element to attach the gutter icon to
   * @return a line marker info that supports multi-marker popup navigation on merged lines
   */
  static <T> @NotNull RelatedItemLineMarkerInfo<PsiElement> createFrom(
      @NotNull NavigationGutterIconBuilder<T> builder,
      @NotNull PsiElement element) {
    NavigationGutterIconRenderer renderer = builder.createGutterIconRenderer(element.getProject(), null);
    GutterIconNavigationHandler<PsiElement> navHandler = renderer.isNavigateAction() ? renderer : null;
    String tooltip = renderer.getTooltipText();

    // The GotoRelatedItem targets come from the renderer, which already holds the target pointers.
    // The factory is lazy, so the targets resolve only when Navigate | Related Symbol asks for them.
    return new NonPersistentLineMarkerInfo<>(
        element,
        element.getTextRange(),
        renderer.getIcon(),
        tooltip == null ? null : e -> tooltip,
        navHandler,
        renderer.getAlignment(),
        () -> GotoRelatedItem.createItems(renderer.getTargetElements(), "Guice"));
  }
}
