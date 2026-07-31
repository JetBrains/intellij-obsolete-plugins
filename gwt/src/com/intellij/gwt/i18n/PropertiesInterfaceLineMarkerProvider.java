/*
 * Copyright 2000-2006 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.gwt.i18n;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.codeInsight.navigation.GutterIconMultipleNavigationHandler;
import com.intellij.gwt.codeInsight.navigation.MultipleNavigationLineMarkerFactory;
import com.intellij.icons.AllIcons;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public final class PropertiesInterfaceLineMarkerProvider extends RelatedItemLineMarkerProvider {
  private static final GutterIconMultipleNavigationHandler<PsiMethod>
    PROPERTY_GUTTER_ICON_NAVIGATION_HANDLER = new GutterIconMultipleNavigationHandler<>() {
    @Override
    public @NotNull String getPopupChooserTitle(final PsiMethod source, final List<? extends GotoRelatedItem> targets) {
      return GwtBundle.message("i18n.goto.property.popup.title", source.getName(), targets.size());
    }

    @Override
    public String getGutterTooltip(final PsiMethod source, List<? extends GotoRelatedItem> targets) {
      return GwtBundle.message("i18n.interface.method.gutter.tooltip", targets.size());
    }
  };
  private static final GutterIconMultipleNavigationHandler<PsiClass>
    GUTTER_ICON_NAVIGATION_HANDLER_PROPERTIES_CLASS = new GutterIconMultipleNavigationHandler<>() {
    @Override
    public @NotNull String getPopupChooserTitle(final PsiClass source, final List<? extends GotoRelatedItem> targets) {
      return GwtBundle.message("i18n.goto.property.popup.title", source.getName(), targets.size());
    }

    @Override
    public String getGutterTooltip(final PsiClass source, List<? extends GotoRelatedItem> targets) {
      return GwtBundle.message("i18n.class.gutter.tooltip.text", targets.size());
    }
  };
  private static final Function<IProperty,PsiElement> MAPPING = iProperty -> iProperty.getPsiElement();
  private static final Function<PropertiesFile,PsiElement> PROPERTIES_FILE_PSI_ELEMENT_FUNCTION =
    propertiesFile -> propertiesFile.getContainingFile();

  @Override
  public void collectNavigationMarkers(@NotNull List<? extends PsiElement> elements,
                                       @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result,
                                       boolean forNavigation) {
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < elements.size(); i++) {
      ProgressManager.checkCanceled();
      PsiElement element = elements.get(i);
      PsiElement parent = element.getParent();

      if (parent instanceof PsiMethod method && method.getNameIdentifier() == element) {
        GwtI18nManager manager = GwtI18nManager.getInstance(method.getProject());
        final IProperty[] properties = manager.getProperties(method);
        if (properties.length != 0) {
          List<GotoRelatedItem> items = GotoRelatedItem.createItems(ContainerUtil.map(properties, MAPPING));
          result.add(MultipleNavigationLineMarkerFactory.create(method, (PsiIdentifier)element, AllIcons.Gutter.ImplementedMethod, items,
                                                                PROPERTY_GUTTER_ICON_NAVIGATION_HANDLER, false));
        }
      }
      if (parent instanceof PsiClass aClass && aClass.getNameIdentifier() == element) {
        final PropertiesFile[] files = GwtI18nManager.getInstance(aClass.getProject()).getPropertiesFiles(aClass);
        if (files.length != 0) {
          List<GotoRelatedItem> items = GotoRelatedItem.createItems(ContainerUtil.map(files, PROPERTIES_FILE_PSI_ELEMENT_FUNCTION));
          result.add(MultipleNavigationLineMarkerFactory.create(aClass, (PsiIdentifier)element, AllIcons.Gutter.ImplementedMethod, items,
                                                                GUTTER_ICON_NAVIGATION_HANDLER_PROPERTIES_CLASS, false));
        }
      }
    }
  }
}
