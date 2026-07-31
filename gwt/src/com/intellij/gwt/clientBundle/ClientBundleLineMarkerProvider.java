package com.intellij.gwt.clientBundle;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.clientBundle.jam.ClientBundleMethodJamElement;
import com.intellij.gwt.clientBundle.jam.CssResourceClassJamElement;
import com.intellij.gwt.clientBundle.jam.CssResourceMethodJamElement;
import com.intellij.gwt.codeInsight.navigation.GutterIconMultipleNavigationHandler;
import com.intellij.gwt.codeInsight.navigation.MultipleNavigationLineMarkerFactory;
import com.intellij.icons.AllIcons;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.css.CssClass;
import com.intellij.psi.css.CssElement;
import com.intellij.psi.css.StylesheetFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class ClientBundleLineMarkerProvider extends RelatedItemLineMarkerProvider {
  public static final GutterIconMultipleNavigationHandler<PsiMethod> RES_METHOD_NAV_HANDLER = new GutterIconMultipleNavigationHandler<>() {
    @Override
    public @NotNull String getPopupChooserTitle(PsiMethod source, List<? extends GotoRelatedItem> targets) {
      return GwtBundle.message("popup.title.resource.method.goto", source.getName(), targets.size());
    }

    @Override
    public String getGutterTooltip(PsiMethod source, List<? extends GotoRelatedItem> targets) {
      return GwtBundle.message("tooltip.text.resource.method.gutter", targets.size());
    }
  };

  private static final GutterIconMultipleNavigationHandler<PsiMethod> CSS_METHOD_NAV_HANDLER = new GutterIconMultipleNavigationHandler<>() {
    @Override
    public @NotNull String getPopupChooserTitle(PsiMethod source, List<? extends GotoRelatedItem> targets) {
      return GwtBundle.message("popup.title.css.class.method.goto", source.getName(), targets.size());
    }

    @Override
    public String getGutterTooltip(PsiMethod source, List<? extends GotoRelatedItem> targets) {
      if (cssClassesOnly(targets)) {
        return GwtBundle.message("tooltip.text.css.class.method.gutter", targets.size());
      }
      else {
        return GwtBundle.message("tooltip.text.css.declaration.method.gutter", targets.size());
      }
    }

    private boolean cssClassesOnly(List<? extends GotoRelatedItem> targets) {
      for (GotoRelatedItem target : targets) {
        if (!(target.getElement() instanceof CssClass)) {
          return false;
        }
      }
      return true;
    }
  };

  private static final GutterIconMultipleNavigationHandler<PsiClass> CSS_CLASS_NAV_HANDLER = new GutterIconMultipleNavigationHandler<>() {
    @Override
    public @NotNull String getPopupChooserTitle(PsiClass source, List<? extends GotoRelatedItem> targets) {
      return GwtBundle.message("popup.title.css.file.goto", source.getName(), targets.size());
    }

    @Override
    public String getGutterTooltip(PsiClass source, List<? extends GotoRelatedItem> targets) {
      return GwtBundle.message("tooltip.text.css.file.gutter", targets.size());
    }
  };


  @Override
  public void collectNavigationMarkers(@NotNull List<? extends PsiElement> elements,
                                       @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result,
                                       boolean forNavigation) {
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < elements.size(); i++) {
      PsiElement element = elements.get(i);
      PsiElement parent = element.getParent();
      if (parent instanceof PsiClass && ((PsiClass)parent).getNameIdentifier() == element) {
        processClass((PsiClass)parent, (PsiIdentifier)element, result);
      }
      else if (parent instanceof PsiMethod && ((PsiMethod)parent).getNameIdentifier() == element) {
        processMethod((PsiMethod)parent, (PsiIdentifier)element, result);
      }
    }
  }

  private static void processMethod(PsiMethod method,
                                    PsiIdentifier identifier,
                                    Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    if (identifier == null) return;

    final ClientBundleMethodJamElement jamElement = ClientBundleMethodJamElement.getElement(method);
    if (jamElement != null) {
      final List<PsiFile> files = jamElement.getSourceFiles(true);
      if (!files.isEmpty()) {
        final Icon icon = files.get(0).getIcon(0);
        result.add(MultipleNavigationLineMarkerFactory
                     .create(method, identifier, icon, GotoRelatedItem.createItems(files), RES_METHOD_NAV_HANDLER,
                             false));
      }
    }

    final CssResourceMethodJamElement methodElement = CssResourceMethodJamElement.getJamElement(method);
    if (methodElement != null) {
      final List<CssElement> declarations = methodElement.findCssElements();
      if (!declarations.isEmpty()) {
        result.add(MultipleNavigationLineMarkerFactory
                     .create(method, identifier, AllIcons.FileTypes.Css, GotoRelatedItem.createItems(declarations), CSS_METHOD_NAV_HANDLER,
                             true));
      }
    }
  }

  private static void processClass(PsiClass psiClass,
                                   PsiIdentifier identifier,
                                   Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    final CssResourceClassJamElement element = CssResourceClassJamElement.getJamElement(psiClass);
    if (element != null) {
      final Set<StylesheetFile> files = element.findStylesheetFiles(true, true);
      if (!files.isEmpty() && identifier != null) {
        result.add(MultipleNavigationLineMarkerFactory
                     .create(psiClass, identifier, AllIcons.FileTypes.Css, GotoRelatedItem.createItems(files), CSS_CLASS_NAV_HANDLER,
                             true));
      }
    }
  }
}


