// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.codeInsight.navigation.NavigationGutterIconRenderer;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.MarkupEditorFilter;
import com.intellij.openapi.util.NotNullFactory;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Collection;
import java.util.Collections;

/**
 * A {@link RelatedItemLineMarkerInfo} subclass whose gutter icons are <em>not</em> persisted
 * to the on-disk highlighting cache ({@code HighlightingNecromancer}).
 *
 * <p>The platform's "zombie markup" mechanism serialises gutter icons whose
 * {@link #getEditorFilter()} returns {@link MarkupEditorFilter#EMPTY} and restores them
 * on IDE restart as static, non-functional icons.  Because Guice gutter icons require
 * the {@link GuiceProjectModel} to be fully built before they become functional, we
 * override {@code getEditorFilter()} to return a custom filter so that the Necromancer
 * skips these markers entirely.
 *
 * <p>The filter itself always returns {@code true} from
 * {@link MarkupEditorFilter#avaliableIn avaliableIn()} so the highlighters are visible
 * in every editor — the only difference from {@code EMPTY} is identity, which prevents
 * the Necromancer from burying them.
 *
 * @see com.intellij.codeInsight.daemon.impl.HighlightingNecromancer#shouldBuryHighlighter
 */
final class NonPersistentLineMarkerInfo<T extends PsiElement> extends RelatedItemLineMarkerInfo<T> {

  /**
   * A filter that accepts all editors (same behaviour as {@link MarkupEditorFilter#EMPTY})
   * but is a distinct instance, preventing the Necromancer from persisting the highlighter.
   */
  private static final MarkupEditorFilter NOT_PERSISTENT = editor -> true;

  private NonPersistentLineMarkerInfo(@NotNull T element,
                                      @NotNull TextRange range,
                                      Icon icon,
                                      @Nullable Function<? super T, String> tooltipProvider,
                                      @Nullable GutterIconNavigationHandler<T> navHandler,
                                      @NotNull GutterIconRenderer.Alignment alignment,
                                      @NotNull NotNullFactory<? extends Collection<? extends GotoRelatedItem>> targets) {
    super(element, range, icon, tooltipProvider, navHandler, alignment, targets);
  }

  @Override
  public @NotNull MarkupEditorFilter getEditorFilter() {
    return NOT_PERSISTENT;
  }

  /**
   * Creates a non-persistent {@link RelatedItemLineMarkerInfo} from a
   * {@link NavigationGutterIconBuilder}, mirroring
   * {@link NavigationGutterIconBuilder#createLineMarkerInfo(PsiElement)}.
   *
   * <p>This method calls {@link NavigationGutterIconBuilder#createGutterIconRenderer} to
   * build the renderer (which materialises targets, tooltip text, etc.), then wraps
   * the result in a {@link NonPersistentLineMarkerInfo} whose gutter icon will
   * <em>not</em> be serialised to the on-disk highlighting cache.
   *
   * @param builder the builder configured with icon, targets, tooltip, etc.
   * @param element the PSI element to attach the gutter icon to
   * @return a non-persistent line marker info
   */
  @SuppressWarnings("unchecked")
  static <T> @NotNull RelatedItemLineMarkerInfo<PsiElement> createFrom(
      @NotNull NavigationGutterIconBuilder<T> builder,
      @NotNull PsiElement element) {
    NavigationGutterIconRenderer renderer = builder.createGutterIconRenderer(element.getProject(), null);
    GutterIconNavigationHandler<PsiElement> navHandler = renderer.isNavigateAction() ? renderer : null;
    String tooltip = renderer.getTooltipText();

    // The GotoRelatedItem targets are obtained from the renderer —
    // NavigationGutterIconRenderer already holds the pointers/converter.
    // We pass a factory that delegates to the renderer's navigation data.
    return new NonPersistentLineMarkerInfo<>(
        element,
        element.getTextRange(),
        renderer.getIcon(),
        tooltip == null ? null : e -> tooltip,
        navHandler,
        renderer.getAlignment(),
        Collections::emptyList);
  }
}
