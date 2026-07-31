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

package com.intellij.gwt.actions;

import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.gwt.templates.GwtTemplates;
import com.intellij.ide.highlighter.HtmlFileType;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.ide.util.PackageUtil;
import com.intellij.javaee.web.WebRoot;
import com.intellij.javaee.web.WebUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.css.CssFileType;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.index.GwtModuleXmlConstants;

import java.util.ArrayList;

public class CreateGwtModuleAction extends GwtCreateActionBase {
  private static final Logger LOG = Logger.getInstance(CreateGwtModuleAction.class);
  private final Ref<String> myHtmlDirectory = Ref.create(null);

  @Override
  protected boolean requireGwtModule() {
    return false;
  }

  @Override
  protected String getDialogPrompt() {
    return GwtBundle.message("new.module.dlg.prompt");
  }

  @Override
  protected String getDialogTitle() {
    return GwtBundle.message("new.module.dlg.title");
  }

  @Override
  protected void doCheckBeforeCreate(final String name, final PsiDirectory directory) throws IncorrectOperationException {
    String[] names = name.split("\\.");
    for (String id : names) {
      PsiUtil.checkIsIdentifier(directory.getManager(), id);
    }
  }

  @Override
  protected void showDialog(@NotNull GwtFacet facet, @NotNull PsiDirectory directory, @NotNull MyInputValidator validator) {
    if (!facet.getSdkVersion().isHtmlFilesOutsideSourcesAreAllowed()) {
      super.showDialog(facet, directory, validator);
      return;
    }

    final CreateGwtModuleDialog dialog = new CreateGwtModuleDialog(getDialogTitle(), facet, validator, directory, myHtmlDirectory);
    dialog.show();
  }

  @Override
  protected PsiElement @NotNull [] doCreate(String name, PsiDirectory directory, final GwtModule gwtModule) throws Exception {
    JavaDirectoryService javaDirectoryService = JavaDirectoryService.getInstance();
    PsiPackage psiPackage = javaDirectoryService.getPackage(directory);
    if (psiPackage == null) return PsiElement.EMPTY_ARRAY;

    int dot = name.indexOf('.');
    if (dot != -1) {
      directory = getRootDirectory(directory);

      while (dot != -1) {
        String directoryName = name.substring(0, dot);
        directory = PackageUtil.findOrCreateSubdirectory(directory, directoryName);
        name = name.substring(dot+1);
        dot = name.indexOf('.');
      }
      psiPackage = javaDirectoryService.getPackage(directory);
      if (psiPackage == null) return PsiElement.EMPTY_ARRAY;
    }

    String moduleName = StringUtil.capitalize(name);
    final ArrayList<PsiElement> res = new ArrayList<>();

    GwtFacet gwtFacet = GwtFacet.findFacetBySourceFile(directory.getProject(), directory.getVirtualFile());
    LOG.assertTrue(gwtFacet != null);

    PsiDirectory client = PackageUtil.findOrCreateSubdirectory(directory, GwtModuleXmlConstants.DEFAULT_SOURCE_PATH);
    res.add(client);
    final PsiClass entryPointClass = createClassFromTemplate(client, moduleName, JavaFileType.INSTANCE, GwtTemplates.GWT_ENTRY_POINT_JAVA);

    final GwtVersion version = gwtFacet.getSdkVersion();
    String appPackageName = psiPackage.getQualifiedName();
    res.add(createFromTemplateInternal(directory, moduleName, moduleName + GwtModuleXmlConstants.GWT_XML_SUFFIX,
                                       XmlFileType.INSTANCE, version.getGwtModuleXmlTemplate(),
                                       GwtTemplates.GWT_MODULE_DOCTYPE_VAR, version.getGwtModuleDocTypeString(),
                                       "ENTRY_POINT_CLASS", entryPointClass.getQualifiedName()));

    PsiDirectory server = PackageUtil.findOrCreateSubdirectory(directory, "server");
    res.add(server);

    PsiDirectory htmlDirectory = null;
    final String htmlDirectoryPath = myHtmlDirectory.get();
    if (version.isHtmlFilesOutsideSourcesAreAllowed()) {
      if (htmlDirectoryPath != null) {
        VirtualFile virtualHtmlDirectory = VfsUtil.createDirectoryIfMissing(htmlDirectoryPath);
        if (virtualHtmlDirectory == null) {
          throw new Exception("Cannot create directory '" + myHtmlDirectory + "'");
        }
        htmlDirectory = PsiManager.getInstance(gwtFacet.getModule().getProject()).findDirectory(virtualHtmlDirectory);
        if (htmlDirectory == null) {
          throw new Exception("Cannot find directory '" + virtualHtmlDirectory.getPath() + "'");
        }
      }
    }
    else {
      htmlDirectory = PackageUtil.findOrCreateSubdirectory(directory, GwtModuleXmlConstants.DEFAULT_PUBLIC_PATH);
    }

    if (htmlDirectory != null) {
      final String gwtModuleName = !appPackageName.isEmpty() ? appPackageName + "." + moduleName : moduleName;
      String gwtModuleHtml = version.getGwtModuleHtmlTemplate();
      String gwtModulePath = gwtModuleName;
      if (version.isHtmlFilesOutsideSourcesAreAllowed()) {
        final WebFacet webFacet = gwtFacet.getWebFacet();
        if (webFacet != null) {
          final WebRoot webRoot = WebUtil.findParentWebRoot(htmlDirectory.getVirtualFile(), webFacet.getWebRoots());
          if (webRoot != null) {
            gwtModulePath = gwtModuleName + "/" + gwtModuleName;//todo prepend with ../../.. path if needed?
          }
        }
      }
      res.add(createFromTemplate(htmlDirectory, moduleName + "." + HtmlFileType.INSTANCE.getDefaultExtension(), HtmlFileType.INSTANCE, gwtModuleHtml,
                                 "GWT_MODULE_NAME", gwtModuleName,
                                 "GWT_MODULE_PATH", gwtModulePath));
      res.add(createFromTemplate(htmlDirectory, moduleName + ".css", CssFileType.INSTANCE, GwtTemplates.GWT_MODULE_CSS));
    }

    res.add(entryPointClass);

    return PsiUtilCore.toPsiElementArray(res);
  }

  public static @NotNull PsiDirectory getRootDirectory(@NotNull PsiDirectory directory) {
    final JavaDirectoryService service = JavaDirectoryService.getInstance();
    PsiPackage aPackage;
    while ((aPackage = service.getPackage(directory)) != null && aPackage.getParentPackage() != null) {
      final PsiDirectory parentDirectory = directory.getParentDirectory();
      if (parentDirectory == null) break;
      directory = parentDirectory;
    }
    return directory;
  }

  @Override
  protected @NotNull String getActionName(@NotNull PsiDirectory directory, @NotNull String newName) {
    return GwtBundle.message("new.module.progress.text", newName);
  }
}
