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

package com.intellij.gwt.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.gwt.maven.GwtLtgtModuleWatcher;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.psi.GwtSourcePathsRefresher;
import com.intellij.gwt.sdk.GwtSdk;
import com.intellij.gwt.sdk.GwtSdkManager;
import com.intellij.gwt.sdk.GwtSdkUtil;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.gwt.sdk.impl.GwtVersionImpl;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.impl.OrderEntryUtil;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainerFactory;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Comparator.comparing;

public class GwtFacet extends Facet<GwtFacetConfiguration> {
  public GwtFacet(final @NotNull FacetType facetType, final @NotNull Module module, final @NlsSafe String name, final @NotNull GwtFacetConfiguration configuration) {
    super(facetType, module, name, configuration, null);
  }                                    

  public static @Nullable GwtFacet getInstance(@NotNull Module module) {
    return FacetManager.getInstance(module).getFacetByType(GwtFacetType.ID);
  }
  
  public static @Nullable GwtFacet findFacetByPsiElement(@Nullable PsiElement element) {
    if (element == null) return null;

    final PsiFile containingFile = element.getContainingFile();
    if (containingFile == null) return null;

    return findFacetBySourceFile(element.getProject(), containingFile.getOriginalFile().getVirtualFile());
  }

  public static @Nullable GwtFacet findFacetBySourceFile(@NotNull Project project, @Nullable VirtualFile file) {
    if (file == null) return null;

    final Module module = ModuleUtilCore.findModuleForFile(file, project);
    if (module == null || module.isDisposed()) return null;

    return getInstance(module);
  }

  public static boolean isInModuleWithGwtFacet(final @NotNull Project project, final @Nullable VirtualFile file) {
    return findFacetBySourceFile(project, file) != null;
  }

  public @Nullable WebFacet getWebFacet() {
    final String webFacetName = getConfiguration().getWebFacetName();
    return webFacetName != null ? FacetManager.getInstance(getModule()).findFacet(WebFacet.ID, webFacetName) : null;
  }

  public @NotNull GwtVersion getSdkVersion() {
    return getConfiguration().getSdk().getVersion();
  }

  @Override
  public void initFacet() {
    GwtFacetConfiguration configuration = getConfiguration();
    GwtSdkManager.getInstance().registerGwtSdk(configuration.getSdk());

    // initialize psi event listeners
    Project project = getModule().getProject();
    GwtLtgtModuleWatcher.getInstance(project);
    GwtSourcePathsRefresher.getInstance(project);
  }

  public static GwtFacet createNewFacet(final @NotNull Module module, final GwtSdk sdk) {
    FacetManager facetManager = FacetManager.getInstance(module);
    final ModifiableFacetModel model = facetManager.createModifiableModel();
    GwtFacet facet = model.getFacetByType(GwtFacetType.ID);
    if (facet != null) return facet;

    GwtFacetType type = GwtFacetType.getInstance();
    GwtFacetConfiguration configuration = ProjectFacetManager.getInstance(module.getProject()).createDefaultConfiguration(type);
    facet = facetManager.createFacet(type, type.getDefaultFacetName(), configuration, null);
    model.addFacet(facet);

    final ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
    setupGwtSdkAndLibraries(configuration, rootModel, sdk);

    WriteAction.run(() -> {
      model.commit();
      rootModel.commit();
    });
    return facet;
  }

  public static void setupGwtSdkAndLibraries(final GwtFacetConfiguration configuration,
                                             ModifiableRootModel rootModel,
                                             @Nullable GwtSdk gwtSdk) {
    setupGwtSdkAndLibraries(configuration, rootModel, gwtSdk, LibrariesContainerFactory.createContainer(rootModel.getProject()));
  }

  public static void setupGwtSdkAndLibraries(final GwtFacetConfiguration configuration, ModifiableRootModel rootModel,
                                             @Nullable GwtSdk gwtSdk, final @NotNull LibrariesContainer container) {
    if (gwtSdk == null || !gwtSdk.isValid()) {
      ProjectFacetManager facetManager = ProjectFacetManager.getInstance(rootModel.getProject());
      gwtSdk = facetManager.getFacets(GwtFacetType.ID).stream()
        .map(GwtFacet::getConfiguration)
        .map(GwtFacetConfiguration::getSdk)
        .filter(GwtSdk::isValid)
        .max(comparing(sdk -> ((GwtVersionImpl)sdk.getVersion())))
        .orElseGet(GwtSdkManager.getInstance()::suggestGwtSdk);
    }

    if (gwtSdk != null) {
      configuration.setGwtSdkUrlAndType(gwtSdk.getHomeDirectoryUrl());
      GwtSdkManager.getInstance().moveToTop(gwtSdk);
      VirtualFile userJar = gwtSdk.getUserJar();
      if (userJar != null) {
        Library library = GwtSdkUtil.findOrCreateGwtUserLibrary(container, userJar);
        if (OrderEntryUtil.findLibraryOrderEntry(rootModel, library) == null) {
          rootModel.addLibraryEntry(library);
        }
      }
    }
  }

  public static @NotNull GwtVersion getGwtVersion(final @Nullable GwtFacet gwtFacet) {
    return gwtFacet != null ? gwtFacet.getSdkVersion() : GwtVersionImpl.getDefaultVersion();
  }

  public static @NotNull GwtVersion getGwtVersion(@Nullable Module module) {
    final GwtFacet facet = module != null ? getInstance(module): null;
    return getGwtVersion(facet);
  }

  public static @Nullable GwtFacet getInstance(GwtModule gwtModule) {
    final Module module = gwtModule.getModule();
    return module != null ? getInstance(module) : null;
  }
}
