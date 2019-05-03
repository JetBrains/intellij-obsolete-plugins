/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.gutter;

import com.intellij.icons.AllIcons;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.struts.StrutsModel;
import com.intellij.struts.dom.Action;
import com.intellij.struts.dom.Controller;
import com.intellij.struts.dom.FormBean;
import com.intellij.struts.dom.PlugIn;
import com.intellij.struts.facet.StrutsFacet;
import com.intellij.util.xml.DomElement;
import icons.StrutsApiIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * Provides annotations and gutter mark icons for Struts-related classes.
 *
 * @author Dmitry Avdeev
 */
public class StrutsClassAnnotator implements Annotator {

  private final ClassAnnotator[] myClassAnnotators =
    new ClassAnnotator[]{
      new ClassAnnotator("org.apache.struts.action.Action", StrutsApiIcons.ActionMapping) {

        @Override
        @Nullable
        protected DomElement[] getDestinations(final PsiClass clazz) {
          final ArrayList<DomElement> destinations = new ArrayList<>();
          final StrutsModel model = getCombinedStrutsModel(clazz);
          if (model != null) {
            for (final Action action : model.getActions()) {
              if (action.getType().getValue() == clazz) {
                destinations.add(action);
              }
            }
          }
          return destinations.isEmpty() ? null : destinations.toArray(DomElement.EMPTY_ARRAY);
        }
      },

      new ClassAnnotator("org.apache.struts.action.ActionForm",
                         StrutsApiIcons.FormBean) {

        @Override
        @Nullable
        protected DomElement[] getDestinations(final PsiClass clazz) {
          final ArrayList<DomElement> destinations = new ArrayList<>();
          final StrutsModel model = getCombinedStrutsModel(clazz);
          if (model != null) {
            for (final FormBean formBean : model.getFormBeans()) {
              if (formBean.getType().getValue() == clazz) {
                destinations.add(formBean);
              }
            }
          }
          return destinations.isEmpty() ? null : destinations.toArray(DomElement.EMPTY_ARRAY);
        }
      },

      new ClassAnnotator("org.apache.struts.action.RequestProcessor",
                         StrutsApiIcons.Controller) {

        @Override
        @Nullable
        protected DomElement[] getDestinations(final PsiClass clazz) {
          final StrutsModel model = getCombinedStrutsModel(clazz);
          if (model != null) {
            final Controller controller = model.getMergedModel().getController();
            final PsiClass processorClazz = controller.getProcessorClass().getValue();
            if (processorClazz != null && processorClazz == clazz) {
              return new DomElement[]{controller};
            }
          }
          return null;
        }
      },

      new ClassAnnotator("org.apache.struts.action.PlugIn",
                         AllIcons.Nodes.Plugin) {

        @Override
        @Nullable
        protected DomElement[] getDestinations(final PsiClass clazz) {
          final ArrayList<DomElement> destinations = new ArrayList<>();
          final StrutsModel model = getCombinedStrutsModel(clazz);
          if (model != null) {
            for (final PlugIn plugIn : model.getMergedModel().getPlugIns()) {
              if (plugIn.getClassName().getValue() == clazz) {
                destinations.add(plugIn);
              }
            }
          }
          return destinations.isEmpty() ? null : destinations.toArray(DomElement.EMPTY_ARRAY);
        }
      }

    };

  @Override
  public void annotate(@NotNull final PsiElement psiElement, @NotNull final AnnotationHolder holder) {
    if (psiElement instanceof PsiClass &&
        StrutsFacet.isPresentForContainingWebFacet(psiElement)) {
      for (final ClassAnnotator classAnnotator : myClassAnnotators) {
        if (classAnnotator.annotate((PsiClass) psiElement, holder)) {
          break;
        }
      }
    }
  }

}
