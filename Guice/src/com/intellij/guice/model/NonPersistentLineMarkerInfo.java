// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.MergeableLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.codeInsight.navigation.NavigationGutterIconRenderer;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.NotNullFactory;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Point;
import java.awt.event.MouseEvent;
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
   * Returns the item for this marker in the popup of a merged gutter icon.
   * Two Guice markers on one line, for example two constructor parameters, merge into one icon.
   *
   * <p>The platform action passes the mouse event of the merged popup to the navigation handler.
   * The action runs after that popup closes, so the event component is not showing.
   * A popup for several targets cannot show relative to that component, and the click does nothing.
   * This action moves the event to the editor at the same screen point.
   *
   * <p>Remove this once IJPL-257312 is fixed.
   *
   * @see MergeableLineMarkerInfo#getNavigateAction()
   */
  @Override
  protected @NotNull AnAction getNavigateAction() {
    AnAction platformAction = super.getNavigateAction();
    return new AnAction() {
      @Override
      public void update(@NotNull AnActionEvent e) {
        platformAction.update(e);
      }

      @Override
      public @NotNull ActionUpdateThread getActionUpdateThread() {
        return platformAction.getActionUpdateThread();
      }

      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        GutterIconNavigationHandler<T> handler = getNavigationHandler();
        if (handler != null) {
          handler.navigate(toShowingMouseEvent(e), getElement());
        }
      }
    };
  }

  private static @NotNull MouseEvent toShowingMouseEvent(@NotNull AnActionEvent e) {
    if (e.getInputEvent() instanceof MouseEvent mouseEvent) {
      if (mouseEvent.getComponent() != null && mouseEvent.getComponent().isShowing()) return mouseEvent;
      Editor editor = e.getData(CommonDataKeys.EDITOR);
      if (editor != null && editor.getContentComponent().isShowing()) {
        JComponent component = editor.getContentComponent();
        // The event computed the screen point when its component was showing.
        Point point = mouseEvent.getLocationOnScreen();
        SwingUtilities.convertPointFromScreen(point, component);
        return new RelativePoint(component, point).toMouseEvent();
      }
    }
    return JBPopupFactory.getInstance().guessBestPopupLocation(e.getDataContext()).toMouseEvent();
  }

  /**
   * Creates a {@link RelatedItemLineMarkerInfo} from a {@link NavigationGutterIconBuilder},
   * mirroring {@link NavigationGutterIconBuilder#createLineMarkerInfo(PsiElement)}.
   *
   * @param builder the builder configured with icon, targets, tooltip, etc.
   * @param element the PSI element to attach the gutter icon to
   * @return a line marker info that supports multi-marker popup navigation on merged lines
   */
  @SuppressWarnings("unchecked")
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
