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

package com.intellij.gwt.junit;

import com.intellij.execution.JUnitPatcher;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.facet.FacetManager;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.facet.GwtFacetType;
import com.intellij.gwt.make.GwtCompilerPaths;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.run.GwtClasspathUtil;
import com.intellij.gwt.runtime.GwtTestsClassLoader;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbModeBlockedFunctionality;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.util.PathUtil;
import com.intellij.util.PathsList;
import com.intellij.util.descriptors.ConfigFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

public final class GwtJUnitPatcher extends JUnitPatcher {
  private static final Logger LOG = Logger.getInstance(GwtJUnitPatcher.class);
  private static final @NonNls String GWT_ARGS_PROPERTY = "gwt.args";

  @Override
  public void patchJavaParameters(@Nullable Module module, JavaParameters javaParameters) {
    if (module == null) {
      return;
    }

    final GwtFacet facet = FacetManager.getInstance(module).getFacetByType(GwtFacetType.ID);
    if (facet == null) {
      return;
    }

    Project project = module.getProject();

    Boolean success = DumbService.getInstance(project).tryRunReadActionInSmartMode(() -> {
      if (GwtModulesManager.getInstance(project).getGwtModules(module, true).isEmpty()) {
        return false;
      }

      PsiClass baseClass = JavaPsiFacade.getInstance(project).findClass(GwtJUnitConstants.GWT_TEST_CASE_CLASS,
                                                                        module.getModuleWithDependenciesAndLibrariesScope(true));
      if (baseClass == null) {
        LOG.debug(GwtJUnitConstants.GWT_TEST_CASE_CLASS + " class not found");
        return false;
      }

      if (ClassInheritorsSearch.search(baseClass, module.getModuleScope(), true).findFirst() == null) {
        LOG.debug("GWTTestCase inheritors not found in " + module.getName());
        return false;
      }

      return true;
    }, GwtBundle.message("notification.content.unable.to.configure.gwt.unit.tests.during.index.update"), DumbModeBlockedFunctionality.Gwt);
    if (!Boolean.TRUE.equals(success)) {
      return;
    }

    final PathsList classPath = javaParameters.getClassPath();
    final PathsList sources = GwtClasspathUtil.getSourceRootsOfGwtModules(module, false);
    List<String> ignoredUrls = new ArrayList<>();
    for (String path : sources.getPathList()) {
      classPath.addFirst(path);
      final File file = new File(path, "META-INF" + File.separator + "jdoconfig.xml");
      if (file.exists()) {
        try {
          ignoredUrls.add(file.toURI().toURL().toString());
        }
        catch (MalformedURLException ignored) {
        }
      }
    }
    classPath.addFirst(facet.getConfiguration().getSdk().getDevJarPath());
    javaParameters.setUseClasspathJar(false);
    if (!ignoredUrls.isEmpty()) {
      LOG.debug(ignoredUrls.size() + " files will be excluded from classpath:");
      for (String ignoredUrl : ignoredUrls) {
        LOG.debug(" " + ignoredUrl);
      }
      try {
        configureGwtTestsClassLoader(javaParameters, classPath, ignoredUrls);
      }
      catch (IOException e) {
        LOG.error(e);
      }
    }

    GwtVersion sdkVersion = facet.getSdkVersion();
    ParametersList vmParameters = javaParameters.getVMParametersList();
    @NonNls StringBuilder builder = new StringBuilder();
    String gwtArgs = vmParameters.getPropertyValue(GWT_ARGS_PROPERTY);
    boolean haveGenOption = false;
    boolean haveOutOption = false;
    if (gwtArgs != null) {
      final String unquoted = StringUtil.unquoteString(gwtArgs);
      @NonNls List<String> existingOptions = StringUtil.splitHonorQuotes(unquoted, ' ');
      haveGenOption = existingOptions.contains("-gen");
      haveOutOption = existingOptions.contains(sdkVersion.getCompilerOutputDirParameterName());
      builder.append(unquoted).append(' ');
    }

    String testGenPath = GwtCompilerPaths.getTestGenDirectory(module).getAbsolutePath();
    String testOutputPath = GwtCompilerPaths.getTestOutputDirectory(module).getAbsolutePath();
    if (sdkVersion.isHostedModeRequiresWebXml() && !haveOutOption) {
      final WebFacet webFacet = facet.getWebFacet();
      if (webFacet != null) {
        final ConfigFile webXmlDescriptor = webFacet.getWebXmlDescriptor();
        if (webXmlDescriptor != null) {
          File webXml = new File(FileUtil.toSystemDependentName(VfsUtilCore.urlToPath(webXmlDescriptor.getUrl())));
          if (webXml.exists()) {
            try {
              FileUtil.copy(webXml, new File(testOutputPath, "WEB-INF" + File.separator + "web.xml"));
            }
            catch (IOException e) {
              LOG.info(e);
            }
          }
        }
      }
    }
    if (!haveGenOption) {
      builder.append("-gen \"").append(testGenPath).append("\" ");
    }
    if (!haveOutOption) {
      builder.append(sdkVersion.getCompilerOutputDirParameterName()).append(" \"").append(testOutputPath).append('\"');
    }
    @NonNls String prefix = "-D" + GWT_ARGS_PROPERTY + "=";
    vmParameters.replaceOrAppend(prefix, prefix + builder);
  }

  private static void configureGwtTestsClassLoader(JavaParameters javaParameters, PathsList classPath, List<String> ignoredUrls) throws IOException {
    classPath.addFirst(PathUtil.getJarPathForClass(GwtTestsClassLoader.class));

    ParametersList vmParametersList = javaParameters.getVMParametersList();
    vmParametersList.add("-Djava.system.class.loader=" + GwtTestsClassLoader.class.getName());
    vmParametersList.add("-Didea.gwt.ignored.resource.urls=" + String.join("\n", ignoredUrls));
  }
}
