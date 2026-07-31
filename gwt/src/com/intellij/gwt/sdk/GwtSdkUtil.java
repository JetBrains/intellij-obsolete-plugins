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

package com.intellij.gwt.sdk;

import com.intellij.facet.ui.FacetConfigurationQuickFix;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.gwt.GwtBundle;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.gwt.model.impl.sdk.GwtSdkPathUtil;

import javax.swing.JComponent;

public final class GwtSdkUtil {
  public static final @NonNls String GWT_CLASS_NAME = "com.google.gwt.core.client.GWT";
  public static final @NonNls String EMUL_ROOT = "com/google/gwt/emul/";

  public static final @NonNls String GWT_DOWNLOAD_URL = "https://code.google.com/webtoolkit/download.html";
  private static final FacetConfigurationQuickFix DOWNLOAD_GWT_FIX = new FacetConfigurationQuickFix(GwtBundle.message("fix.download.gwt")) {
    @Override
    public void run(final JComponent place) {
      BrowserUtil.browse(GWT_DOWNLOAD_URL);
    }
  };
  public static final @NonNls String GWT_15_COMPILER_MAIN_CLASS = "com.google.gwt.dev.GWTCompiler";
  public static final @NonNls String GWT_16_COMPILER_MAIN_CLASS = "com.google.gwt.dev.Compiler";
  public static final @NonNls String GWT_15_DEV_MODE_CLASS = "com.google.gwt.dev.GWTShell";
  public static final @NonNls String GWT_20_DEV_MODE_CLASS = "com.google.gwt.dev.DevMode";

  private GwtSdkUtil() {
  }

  private static ValidationResult checkClass(final @NonNls String className,
                                             final @Nullable String oldClassName,
                                             String gwtPath,
                                             final String jarPath) {
    final VirtualFile jarFile = JarFileSystem.getInstance().refreshAndFindFileByPath(FileUtil.toSystemIndependentName(jarPath)
                                                                                     + JarFileSystem.JAR_SEPARATOR);
    if (jarFile == null) {
      return invalidGwtInstallation(gwtPath, GwtBundle.message("error.file.not.found.message", jarPath));
    }
    VirtualFile[] files = {jarFile};
    if (!LibraryUtil.isClassAvailableInLibrary(files, className)
      && (oldClassName == null || !LibraryUtil.isClassAvailableInLibrary(files, oldClassName))) {
      return invalidGwtInstallation(gwtPath, GwtBundle.message("error.class.not.found.in.jar", className, jarPath));
    }
    return ValidationResult.OK;
  }

  private static ValidationResult invalidGwtInstallation(final String gwtPath, final String errorMessage) {
    return new ValidationResult(GwtBundle.message("error.invalid.gwt.installation.message", gwtPath, errorMessage), DOWNLOAD_GWT_FIX);
  }

  public static ValidationResult checkGwtSdkPath(final String gwtPath) {
    if (gwtPath.contains("!")) {
      return new ValidationResult(GwtBundle.message("error.message.path.to.gwt.sdk.must.not.contain.character"));
    }

    String devJarPath = GwtSdkPathUtil.getDevJarPath(gwtPath);
    ValidationResult result = checkClass(GWT_16_COMPILER_MAIN_CLASS, GWT_15_COMPILER_MAIN_CLASS, gwtPath, devJarPath);

    if (result.isOk()) {
      result = checkClass(GWT_20_DEV_MODE_CLASS, GWT_15_DEV_MODE_CLASS, gwtPath, devJarPath);
    }
    if (result.isOk()) {
      result = checkClass(GWT_CLASS_NAME, null, gwtPath, GwtSdkPathUtil.getUserJarPath(gwtPath));
    }
    return result;
  }

  public static Library findOrCreateGwtUserLibrary(final @NotNull LibrariesContainer container, final @NotNull VirtualFile userJar) {
    final Library library = findLibrary(container, userJar);
    if (library != null) {
      return library;
    }

    return container.createLibrary("gwt-user", LibrariesContainer.LibraryLevel.PROJECT, new VirtualFile[]{userJar}, new VirtualFile[]{userJar});
  }

  private static @Nullable Library findLibrary(final LibrariesContainer container, final VirtualFile userJar) {
    for (Library library : container.getAllLibraries()) {
      final VirtualFile[] files = library.getFiles(OrderRootType.CLASSES);
      for (VirtualFile file : files) {
        if (userJar.equals(file)) {
          return library;
        }
      }
    }
    return null;
  }

  public static String getJreEmulationClassPath(String className) {
    return EMUL_ROOT + className.replace('.', '/') + "." + JavaFileType.INSTANCE.getDefaultExtension();
  }

  public static @Nullable VirtualFile getUserJar(final String homePath) {
    return findJarFile(GwtSdkPathUtil.getUserJarPath(homePath));
  }

  public static @Nullable VirtualFile findJarFile(String jarPath) {
    return JarFileSystem.getInstance().findFileByPath(FileUtil.toSystemIndependentName(jarPath) + JarFileSystem.JAR_SEPARATOR);
  }
}
