// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.tagSupport;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.xml.XmlAttributeValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.GspFileViewProvider;
import org.jetbrains.plugins.grails.references.common.GspTagWrapper;
import org.jetbrains.plugins.grails.references.controller.ActionReference;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

public class GspActionAttributeSupport extends TagAttributeReferenceProvider {

  public GspActionAttributeSupport() {
    super("action", "g", null);
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                         @NotNull String text,
                                                         int offset,
                                                         @NotNull GspTagWrapper gspTagWrapper) {
    String controllerName;

    PsiElement attributeController = gspTagWrapper.getAttributeValue("controller");
    if (attributeController == null) {
      PsiFile psiFile = element.getContainingFile();
      if (psiFile == null) return PsiReference.EMPTY_ARRAY;

      VirtualFile virtualFile = psiFile.getOriginalFile().getVirtualFile();
      if (virtualFile == null) return PsiReference.EMPTY_ARRAY;

      if (psiFile.getViewProvider() instanceof GspFileViewProvider) {
        controllerName = GrailsUtils.getControllerNameByGsp(virtualFile);
        if (controllerName == null) return PsiReference.EMPTY_ARRAY;
      }
      else {
        PsiClass controllerClass = PsiUtil.getContainingNotInnerClass(element);
        if (!GrailsArtifact.CONTROLLER.isInstance(controllerClass)) return PsiReference.EMPTY_ARRAY;
        assert controllerClass != null;
        controllerName = GrailsArtifact.CONTROLLER.getArtifactName(controllerClass);
      }
    }
    else {
      controllerName = gspTagWrapper.getAttributeText(attributeController);
      if (controllerName == null) {
        return PsiReference.EMPTY_ARRAY;
      }
    }

    return new PsiReference[]{new ActionReference(element, element instanceof XmlAttributeValue, controllerName)};
  }

}
