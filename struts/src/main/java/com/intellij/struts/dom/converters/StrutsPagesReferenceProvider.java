/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.dom.converters;

import com.intellij.javaee.web.WebUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.paths.PathReference;
import com.intellij.openapi.paths.PathReferenceProvider;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.jsp.WebDirectoryUtil;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.struts.StrutsManager;
import com.intellij.struts.StrutsModel;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

/**
 * @author Dmitry Avdeev
*/
public class StrutsPagesReferenceProvider implements PathReferenceProvider {

  @Override
  public boolean createReferences(final @NotNull PsiElement psiElement, final @NotNull List<PsiReference> references, final boolean soft) {
    final FileReferenceSet set = FileReferenceSet.createSet(psiElement, soft, false, true);
    if (set == null) {
      return true;
    }
    set.addCustomization(
      FileReferenceSet.DEFAULT_PATH_EVALUATOR_OPTION,
      file -> {
        final StrutsModel model = StrutsManager.getInstance().getStrutsModel(psiElement);
        return model == null ? Collections.emptyList() : ContainerUtil.createMaybeSingletonList(model.getModuleRoot());
      });
    Collections.addAll(references, set.getAllReferences());
    return false;
  }

  @Override
  @Nullable
  public PathReference getPathReference(@NotNull final String path, @NotNull final PsiElement element) {
    final WebFacet webFacet = WebUtil.getWebFacet(element);
    if (webFacet == null) return null;

    final WebDirectoryUtil webDirectoryUtil = WebDirectoryUtil.getWebDirectoryUtil(element.getProject());
    final PsiElement psiElement = webDirectoryUtil.findFileByPath(path, webFacet);
    final Function<PathReference, Icon> iconFunction = webPath -> psiElement.getIcon(Iconable.ICON_FLAG_READ_STATUS);

    return psiElement != null ? new PathReference(path, iconFunction) {
      @Override
      public PsiElement resolve() {
        return webDirectoryUtil.findFileByPath(path, webFacet);
      }
    } : null;
  }
}
