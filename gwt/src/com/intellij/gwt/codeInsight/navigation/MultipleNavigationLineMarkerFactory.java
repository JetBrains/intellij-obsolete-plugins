package com.intellij.gwt.codeInsight.navigation;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.awt.event.MouseEvent;
import java.util.List;

public final class MultipleNavigationLineMarkerFactory {

  private MultipleNavigationLineMarkerFactory() {
  }

  public static <T extends NavigatablePsiElement> RelatedItemLineMarkerInfo<PsiElement> create(T element,
                                                                                               @NotNull PsiIdentifier identifier,
                                                                                               final Icon icon,
                                                                                               final List<? extends GotoRelatedItem> targets,
                                                                                               GutterIconMultipleNavigationHandler<T> handler,
                                                                                               boolean showContainingModules) {
    final MultipleNavigationTooltipProvider<T> tooltipProvider = new MultipleNavigationTooltipProvider<>(handler, element, targets);
    final MultipleNavigationHandler<T> navigateHandler = new MultipleNavigationHandler<>(handler, element, targets, showContainingModules);
    return new RelatedItemLineMarkerInfo<>(identifier, identifier.getTextRange(), icon, tooltipProvider, navigateHandler,
                                           GutterIconRenderer.Alignment.CENTER, ()->targets);
  }

  private static class MultipleNavigationHandler<T extends PsiElement> implements GutterIconNavigationHandler<PsiElement> {
    private final GutterIconMultipleNavigationHandler<T> myGutterIconNavigationHandler;
    private final T mySource;
    private final List<? extends GotoRelatedItem> myTargets;
    private final boolean myShowContainingModules;

    MultipleNavigationHandler(final GutterIconMultipleNavigationHandler<T> gutterIconNavigationHandler, final T source,
                              final List<? extends GotoRelatedItem> targets, final boolean showContainingModules) {
      myGutterIconNavigationHandler = gutterIconNavigationHandler;
      mySource = source;
      myTargets = targets;
      myShowContainingModules = showContainingModules;
    }

    @Override
    public void navigate(MouseEvent e, PsiElement elt) {
      if (myTargets.size() == 1) {
        myTargets.get(0).navigate();
      }
      else {
        final String title = myGutterIconNavigationHandler.getPopupChooserTitle(mySource, myTargets);
        final JBPopup popup = NavigationUtil.getRelatedItemsPopup(myTargets, title, myShowContainingModules);
        popup.show(new RelativePoint(e));
      }
    }
  }

  private static class MultipleNavigationTooltipProvider<T extends PsiElement> implements Function<PsiElement, String> {
    private final GutterIconMultipleNavigationHandler<T> myHandler;
    private final T mySource;
    private final List<? extends GotoRelatedItem> myTargets;

    MultipleNavigationTooltipProvider(GutterIconMultipleNavigationHandler<T> handler, T source, List<? extends GotoRelatedItem> targets) {
      myHandler = handler;
      mySource = source;
      myTargets = targets;
    }

    @Override
    public String fun(PsiElement s) {
      return myHandler.getGutterTooltip(mySource, myTargets);
    }
  }
}
