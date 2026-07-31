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
import com.intellij.gwt.module.model.GwtServlet;
import com.intellij.gwt.rpc.GwtServletUtil;
import com.intellij.gwt.rpc.RemoteServiceUtil;
import com.intellij.gwt.templates.GwtTemplates;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.javaee.web.WebUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.javaee.web.model.xml.WebApp;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.xml.DomUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CreateGwtRemoteServiceAction extends GwtCreateActionBase {
  private static final Logger LOG = Logger.getInstance(CreateGwtRemoteServiceAction.class);
  private static final @NonNls String QUALIFIED_SERVICE_NAME_PROPERTY = "QUALIFIED_SERVICE_NAME";
  private static final @NonNls String SERVICE_NAME_PROPERTY = "SERVICE_NAME";
  private static final @NonNls String SERVLET_PATH_PROPERTY = "SERVLET_PATH";
  private static final @NonNls String RELATIVE_PATH_PROPERTY = "RELATIVE_SERVLET_PATH";
  private static final @NonNls String SERVER_PACKAGE = "server";

  @Override
  protected boolean requireGwtModule() {
    return true;
  }

  @Override
  protected String getDialogPrompt() {
    return GwtBundle.message("new.service.dlg.prompt");
  }

  @Override
  protected String getDialogTitle() {
    return GwtBundle.message("new.service.dlg.title");
  }


  @Override
  protected PsiFile[] getAffectedFiles(final GwtModule gwtModule) {
    final XmlFile xmlFile = gwtModule.getModuleXmlFile();
    if (xmlFile != null) {
      final WebFacet webFacet = WebUtil.getWebFacet(xmlFile);
      if (webFacet != null) {
        final WebApp webApp = webFacet.getRoot();
        if (webApp != null) {
          return new PsiFile[]{xmlFile, DomUtil.getFile(webApp)};
        }
      }
    }
    return new PsiFile[] {xmlFile};
  }

  @Override
  protected PsiElement @NotNull [] doCreate(String serviceName, PsiDirectory directory, final GwtModule gwtModule) throws Exception {
    ArrayList<PsiElement> res = new ArrayList<>(0);

    final String servletPath = GwtServletUtil.getDefaultServletPath(gwtModule, serviceName);
    final GwtFacet gwtFacet = GwtFacet.getInstance(gwtModule);
    LOG.assertTrue(gwtFacet != null);

    final String templateName = gwtFacet.getSdkVersion().getGwtServiceJavaTemplate();
    final PsiClass serviceClass = createClassFromTemplate(directory, serviceName, JavaFileType.INSTANCE, templateName,
                                                          SERVLET_PATH_PROPERTY, servletPath,
                                                          RELATIVE_PATH_PROPERTY, servletPath.substring(1));
    res.add(serviceClass);
    res.add(createClassFromTemplate(directory, serviceName + RemoteServiceUtil.ASYNC_SUFFIX, JavaFileType.INSTANCE, GwtTemplates.GWT_SERVICE_ASYNC_JAVA));


    PsiClass servletImpl = generateServletClass(serviceName, directory, gwtModule, serviceClass);
    if (servletImpl == null) return PsiElement.EMPTY_ARRAY;

    XmlFile xml = gwtModule.getModuleXmlFile();
    if (xml == null) return PsiElement.EMPTY_ARRAY;

    final GwtServlet gwtServlet = gwtModule.addServlet();
    gwtServlet.getPath().setValue(servletPath);
    gwtServlet.getServletClass().setValue(servletImpl.getQualifiedName());

    final WebFacet webFacet = WebUtil.getWebFacet(xml);
    if (webFacet != null) {
      final WebApp webApp = webFacet.getRoot();
      if (webApp != null) {
        GwtServletUtil.registerServletForService(gwtFacet, gwtModule, webApp, servletImpl, serviceName);
      }
    }

    return PsiUtilCore.toPsiElementArray(res);
  }

  @Override
  protected @NotNull String getActionName(@NotNull PsiDirectory directory, @NotNull String newName) {
    return GwtBundle.message("new.service.progress.text", newName);
  }

  private static @Nullable PsiClass generateServletClass(String name, PsiDirectory directory, GwtModule gwtModule,
                                                         final PsiClass serviceClass) throws IncorrectOperationException {
    VirtualFile dir = directory.getVirtualFile();
    List<String> pathFromClient = new ArrayList<>();
    while (gwtModule.isSourceFile(dir)) {
      final String dirName = dir.getName();
      dir = dir.getParent();
      if (gwtModule.isSourceFile(dir)) {
        pathFromClient.add(dirName);
      }
    }
    Collections.reverse(pathFromClient);

    PsiDirectory serverDir = directory.getManager().findDirectory(dir);
    if (serverDir == null) return null;

    pathFromClient.add(0, SERVER_PACKAGE);
    for (String dirName : pathFromClient) {
      PsiDirectory nextDir = serverDir.findSubdirectory(dirName);
      if (nextDir == null) {
        nextDir = serverDir.createSubdirectory(dirName);
      }
      serverDir = nextDir;
    }

    return createClassFromTemplate(serverDir, name + RemoteServiceUtil.IMPL_SERVICE_SUFFIX, JavaFileType.INSTANCE, GwtTemplates.GWT_SERVICE_IMPL_JAVA,
                                   SERVICE_NAME_PROPERTY, serviceClass.getName(),
                                   QUALIFIED_SERVICE_NAME_PROPERTY, serviceClass.getQualifiedName());
  }
}