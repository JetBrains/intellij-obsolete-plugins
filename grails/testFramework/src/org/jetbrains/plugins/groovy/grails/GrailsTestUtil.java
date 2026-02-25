// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.config.GrailsSettings;
import org.jetbrains.plugins.grails.config.GrailsSettingsService;
import org.jetbrains.plugins.groovy.mvc.MvcProjectStructure;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@SuppressWarnings({"AbstractClassNeverImplemented"})
public abstract class GrailsTestUtil {

  private static final String[] DIRS =
    {"grails-app/domain", "grails-app/controllers", "grails-app/services", "grails-app/taglib", "src/java", "src/groovy", "test/unit",
      "test/integration", "web-app", "grails-app/conf/hibernate"};

  public static String getMockGrailsLibraryHome() {
    return getTestRootPath("/testdata/mockGrails11/lib");
  }
  public static String getMockGrails11LibraryHome() {
    return getTestRootPath("/testdata/mockGrails11");
  }

  public static String getMockGrails14LibraryHome() {
    return getTestRootPath("/testdata/mockGrails14");
  }

  public static String getTestRootPath(String directory) {
    return FileUtil.toSystemIndependentName(new File("").getAbsoluteFile() + directory);
  }

  public static void createGrailsApplication(JavaCodeInsightTestFixture fixture) throws IOException {
    createGrailsApplication(fixture, ".");
  }

  public static void createGrailsApplication(final JavaCodeInsightTestFixture fixture, final String prefix) throws IOException {
    createGrailsApplication(fixture, prefix, true);
  }

  public static void createGrailsApplication(final JavaCodeInsightTestFixture fixture, final String prefix, final boolean markAsSource) throws IOException {
    createGrailsApplication(fixture, fixture.getModule(), null, prefix, markAsSource);
  }

  public static void createPluginXml(JavaCodeInsightTestFixture fixture, String path) {
    assert !path.endsWith("/");
    String dir = path.substring(path.lastIndexOf('/') + 1);
    int idx = dir.lastIndexOf('-');
    String pluginName = idx == -1 ? dir : dir.substring(0, idx);

    assert !pluginName.isEmpty();

    fixture.addFileToProject(path + "/plugin.xml", "<plugin name='" + pluginName + "'></plugin>");
  }
  
  public static void createGrailsApplication(final JavaCodeInsightTestFixture fixture,
                                             @NotNull Module module,
                                             final @Nullable ContentEntry entry,
                                             final String prefix,
                                             final boolean markAsSource) throws IOException {
    final TempDirTestFixture tdf = fixture.getTempDirFixture();

    ApplicationManager.getApplication().runWriteAction(() -> {
      ContentEntry currentEntry;
      ModifiableRootModel model = null;

      if (entry == null) {
        model = ModuleRootManager.getInstance(module).getModifiableModel();
        currentEntry = model.getContentEntries()[0];
      }
      else {
        currentEntry = entry;
      }

      MvcProjectStructure structure = GrailsFramework.getInstance().createProjectStructure(module, false);

      Collection<String> sources = structure.getSourceFolders().get(JavaSourceRootType.SOURCE);
      Collection<String> testSources = structure.getSourceFolders().get(JavaSourceRootType.TEST_SOURCE);

      try {
        for (String dir : DIRS) {
          VirtualFile file = tdf.findOrCreateDir(prefix + '/' + dir);

          if (markAsSource && (sources.contains(dir) || testSources.contains(dir))) {
            currentEntry.addSourceFolder(file, testSources.contains(dir));
          }
        }

        if (model != null) model.commit();
      }
      catch (IOException e) {
        if (model != null) model.dispose();
        throw new RuntimeException(e);
      }
    });

    tdf.createFile(prefix + "/application.properties", "");
  }

  public static void createBuildConfig(JavaCodeInsightTestFixture fixture, String prefix, Map<String, String> properties) {
    createBuildConfig(fixture, prefix, properties, Collections.emptyMap());
  }

  public static void createBuildConfig(JavaCodeInsightTestFixture fixture, String prefix, Map<String, String> properties, Map<String, String> customPlugins) {
    createBuildConfig(fixture, prefix, properties, customPlugins, "");
  }

  public static void createBuildConfig(JavaCodeInsightTestFixture fixture, String prefix, Map<String, String> properties, Map<String, String> customPlugins, @NotNull String text) {
    StringBuilder builder = new StringBuilder();

    builder.append(text);
    builder.append('\n');

    for (Map.Entry<String, String> entry : properties.entrySet()) {
      builder.append(entry.getKey()).append("='").append(entry.getValue()).append("'\n");
    }

    for (Map.Entry<String, String> entry : customPlugins.entrySet()) {
      builder.append("grails.plugin.location.").append(entry.getKey()).append("='").append(entry.getValue()).append("'\n");
    }

    String buildConfigText = builder.toString();

    VirtualFile configFile = fixture.getTempDirFixture().createFile(prefix + "/grails-app/conf/BuildConfig.groovy");
    fixture.saveText(configFile, buildConfigText);
    PsiDocumentManager.getInstance(fixture.getProject()).commitAllDocuments();

    ProjectFileIndex fileIndex = ProjectRootManager.getInstance(fixture.getProject()).getFileIndex();

    GrailsSettings settings;

    Module module = fileIndex.getModuleForFile(configFile);
    if (module != null && Comparing.equal(fileIndex.getContentRootForFile(configFile), configFile.getParent().getParent().getParent())) {
      settings = GrailsSettingsService.getGrailsSettings(module);
    }
    else {
      settings = new GrailsSettings();
      configFile.putUserData(GrailsSettingsService.getTestDataKey(), settings);
    }

    settings.properties.putAll(properties);
    settings.customPluginLocations.clear();
    settings.customPluginLocations.putAll(customPlugins);
    settings.updateBuildConfig(buildConfigText);
  }

}
