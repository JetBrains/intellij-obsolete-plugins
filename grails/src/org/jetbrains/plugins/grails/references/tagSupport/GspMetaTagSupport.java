// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.tagSupport;

import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.xml.XmlAttributeValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.common.GspTagWrapper;

import java.util.List;

public class GspMetaTagSupport extends TagAttributeReferenceProvider {

  protected GspMetaTagSupport() {
    super("name", "g", new String[]{"meta"});
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                         @NotNull String text,
                                                         int offset,
                                                         @NotNull GspTagWrapper gspTagWrapper) {
    PsiFile file = element.getContainingFile();
    if (file == null) return PsiReference.EMPTY_ARRAY;

    VirtualFile virtualFile = file.getOriginalFile().getVirtualFile();
    if (virtualFile == null) return PsiReference.EMPTY_ARRAY;

    ProjectFileIndex fileIndex = ProjectRootManager.getInstance(file.getProject()).getFileIndex();
    VirtualFile root = fileIndex.getContentRootForFile(virtualFile);
    if (root == null) return PsiReference.EMPTY_ARRAY;

    VirtualFile properties = root.findChild("application.properties");
    if (properties == null) return PsiReference.EMPTY_ARRAY;

    PsiFile psiPropertiesFile = file.getManager().findFile(properties);
    if (!(psiPropertiesFile instanceof PropertiesFile propertiesFile)) return PsiReference.EMPTY_ARRAY;

    return new PsiReference[]{
      new PsiReferenceBase<>(element, element instanceof XmlAttributeValue) {

        @Override
        public PsiElement resolve() {
          String value = getValue();
          List<IProperty> propertiesList = propertiesFile.findPropertiesByKey(value);
          return propertiesList.isEmpty() ? null : propertiesList.get(0).getPsiElement();
        }

        @Override
        public Object @NotNull [] getVariants() {
          return propertiesFile.getProperties().toArray();
        }
      }
    };
  }
}
