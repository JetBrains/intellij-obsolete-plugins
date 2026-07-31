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

package com.intellij.gwt.module.model;

import com.intellij.gwt.facet.GwtFacet;
import com.intellij.javaee.web.WebDirectoryElement;
import com.intellij.javaee.web.WebUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.css.StylesheetFile;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.Converter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class CssFileConverter extends Converter<StylesheetFile> {
  @Override
  public @Nullable StylesheetFile fromString(final @Nullable String path, final @NotNull ConvertContext context) {
    GwtModule gwtModule = context.getInvocationElement().getParentOfType(GwtModule.class, false);
    if (gwtModule == null || path == null) return null;

    final Collection<VirtualFile> publicRoots = gwtModule.getPublicRoots();

    for (VirtualFile root : publicRoots) {
      final VirtualFile cssFile = root.findFileByRelativePath(path);
      if (cssFile != null) {
        final PsiManager psiManager = context.getPsiManager();
        final PsiFile psiFile = psiManager.findFile(cssFile);
        if (psiFile instanceof StylesheetFile) {
          return (StylesheetFile)psiFile;
        }
      }
    }

    final GwtFacet facet = GwtFacet.getInstance(gwtModule);
    String pathFromWebRoot;
    if (path.startsWith("/")) {
      pathFromWebRoot = path;
    }
    else if (path.startsWith("../")) {
      //GWT compiler output is placed in a subdirectory of web root so relative paths also work
      pathFromWebRoot = path.substring(2);
    }
    else {
      pathFromWebRoot = null;
    }
    if (facet != null && facet.getSdkVersion().isHtmlFilesOutsideSourcesAreAllowed() && pathFromWebRoot != null) {
      final WebFacet webFacet = facet.getWebFacet();
      if (webFacet != null) {
        final WebDirectoryElement element = WebUtil.getWebUtil().findWebDirectoryElement(pathFromWebRoot, webFacet);
        if (element != null) {
          final PsiFile file = element.getOriginalFile();
          if (file instanceof StylesheetFile) {
            return (StylesheetFile)file;
          }
        }
      }
    }

    return null;
  }

  @Override
  public String getErrorMessage(@Nullable String s, @NotNull ConvertContext context) {
    return null;
  }

  @Override
  public String toString(final StylesheetFile stylesheetFile, final @NotNull ConvertContext context) {
    throw new UnsupportedOperationException();
  }
}
