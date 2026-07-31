package com.intellij.gwt.uiBinder;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.codeInsight.navigation.GutterIconMultipleNavigationHandler;
import com.intellij.gwt.codeInsight.navigation.MultipleNavigationLineMarkerFactory;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.uiBinder.mapping.UiBinderMappingService;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.PlatformIcons;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class GwtUiLineMarkerProvider extends RelatedItemLineMarkerProvider {
  private static final GutterIconMultipleNavigationHandler<PsiClass> UI_BINDER_CLASS_NAV_HANDLER =
    new GutterIconMultipleNavigationHandler<>() {
      @Override
      public @NotNull String getPopupChooserTitle(PsiClass source, List<? extends GotoRelatedItem> targets) {
        return GwtBundle.message("popup.title.ui.xml.file.goto", source.getName(), targets.size());
      }

      @Override
      public String getGutterTooltip(PsiClass source, List<? extends GotoRelatedItem> targets) {
        return GwtBundle.message("tooltip.text.ui.xml.file.gutter", targets.size());
      }
    };
  private static final GutterIconMultipleNavigationHandler<PsiField> UI_FIELD_NAV_HANDLER = new GutterIconMultipleNavigationHandler<>() {
    @Override
    public @NotNull String getPopupChooserTitle(PsiField source, List<? extends GotoRelatedItem> targets) {
      return GwtBundle.message("popup.title.ui.tag.file.goto", source.getName(), targets.size());
    }

    @Override
    public String getGutterTooltip(PsiField source, List<? extends GotoRelatedItem> targets) {
      return GwtBundle.message("tooltip.text.ui.tag.file.gutter", targets.size());
    }
  };

  @Override
  public RelatedItemLineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
    PsiElement parent;
    if (element instanceof PsiIdentifier && (parent = element.getParent()) instanceof PsiClass && ((PsiClass)parent).getNameIdentifier() == element) {
      if (GwtFacet.isInModuleWithGwtFacet(element.getProject(), element.getContainingFile().getVirtualFile())) {
        final PsiClass psiClass = (PsiClass)parent;
        List<XmlFile> files = UiBinderMappingService.getUiXmlFilesForClass(psiClass);
        if (!files.isEmpty()) {
          List<GotoRelatedItem> targets = GotoRelatedItem.createItems(files);
          return MultipleNavigationLineMarkerFactory
            .create(psiClass, (PsiIdentifier)element, PlatformIcons.UI_FORM_ICON, targets, UI_BINDER_CLASS_NAV_HANDLER, false);
        }
      }
    }
    return null;
  }

  @Override
  public void collectNavigationMarkers(@NotNull List<? extends PsiElement> elements,
                                       @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result,
                                       boolean forNavigation) {
    for (PsiElement element : elements) {
      PsiElement parent;
      if (!(element instanceof PsiIdentifier) || !((parent = element.getParent()) instanceof PsiField) || ((PsiField)parent).getNameIdentifier() != element) continue;
      final PsiField field = (PsiField)parent;
      if (!UiBinderUtil.isUiField(field)) continue;

      final List<XmlTag> tags = findTagsForField(field);
      if (!tags.isEmpty()) {
        List<? extends GotoRelatedItem> targets = GotoRelatedItem.createItems(tags);
        result.add(MultipleNavigationLineMarkerFactory
                     .create(field, (PsiIdentifier)element, PlatformIcons.UI_FORM_ICON, targets, UI_FIELD_NAV_HANDLER,
                             false));
      }
    }
  }

  private static @NotNull List<XmlTag> findTagsForField(PsiField field) {
    final PsiClass psiClass = field.getContainingClass();
    if (psiClass == null) return Collections.emptyList();

    final List<XmlTag> result = new ArrayList<>();
    for (XmlFile file : UiBinderMappingService.getUiXmlFilesForClass(psiClass)) {
      ContainerUtil.addIfNotNull(result, GwtUiXmlFileUtil.findTagForField(file, field.getName()));
    }
    return result;
  }
}
