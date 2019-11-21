/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.idea.devkit.ant;

import com.intellij.compiler.ant.*;
import com.intellij.compiler.ant.taskdefs.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.idea.devkit.DevKitBundle;
import org.jetbrains.idea.devkit.build.PluginBuildConfiguration;
import org.jetbrains.idea.devkit.build.PluginBuildUtil;
import org.jetbrains.idea.devkit.build.PrepareToDeployAction;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

public class BuildJarTarget extends Target {
  public BuildJarTarget(final ModuleChunk chunk,
                        final GenerationOptions genOptions,
                        final PluginBuildConfiguration moduleBuildProperties) {
    super(PluginBuildProperties.getBuildJarTargetName(chunk.getName()), BuildProperties.getCompileTargetName(chunk.getName()),
          DevKitBundle.message("ant.build.jar.description", chunk.getName()), null);

    final File moduleBaseDir = chunk.getBaseDir();

    final Module[] modules = chunk.getModules();

    final Module module = modules[0];

    final String moduleName = module.getName();


    final HashSet<Library> libs = new HashSet<>();
    for (Module module1 : modules) {
      PluginBuildUtil.getLibraries(module1, libs);
    }

    final String jarPathPropertyRef = BuildProperties.propertyRef(PluginBuildProperties.getJarPathProperty(moduleName));

    if (libs.isEmpty()) {
      add(createPluginsJar(jarPathPropertyRef, modules, moduleBaseDir, genOptions, moduleBuildProperties));
    } else {
      @NonNls final String tempSuffix = "temp";
      final File jarDir = new File(moduleBaseDir.getParentFile(), tempSuffix);
      String tempDir = GenerationUtils.toRelativePath(jarDir.getPath(), chunk, genOptions);
      final String tempDirProperty = BuildProperties.getTempDirForModuleProperty(moduleName);

      add(new Property(tempDirProperty, tempDir));
      add(new Mkdir(BuildProperties.propertyRef(tempDirProperty)));

      add(new Mkdir(BuildProperties.propertyRef(tempDirProperty) + "/lib"));

      final @NonNls String libRelativePath = BuildProperties.propertyRef(tempDirProperty) + "/lib/";

      add(createPluginsJar(libRelativePath + chunk.getName() + ".jar", modules, moduleBaseDir, genOptions, moduleBuildProperties));

      for (Library lib : libs) {
        final VirtualFile[] files = lib.getFiles(OrderRootType.CLASSES);
        for (VirtualFile file : files) {
          final String relativePath = GenerationUtils.toRelativePath(file, chunk, genOptions);
          if (file.getFileSystem() instanceof JarFileSystem) {
            add(new Copy(relativePath, libRelativePath + file.getName()));
          } else {
            final Jar jar = new Jar(libRelativePath + file.getNameWithoutExtension() + ".jar", "preserve");
            jar.add(new ZipFileSet(relativePath, "", true));
            add(jar);
          }
        }
      }

      final Tag zipTag = new Zip(jarPathPropertyRef);
      zipTag.add(new FileSet(tempDir));
      add(zipTag);

      add(new Delete(BuildProperties.propertyRef(tempDirProperty)));
    }
  }

  private static Tag createPluginsJar(@NonNls final String jarPathProperty,
                                      final Module[] modules, final File moduleBaseDir,
                                      final GenerationOptions genOptions,
                                      final PluginBuildConfiguration moduleBuildProperties) {
    final Tag jarTag = new Jar(jarPathProperty, "preserve");
    for (Module m : modules) {
      final String path = VfsUtil.urlToPath(CompilerModuleExtension.getInstance(m).getCompilerOutputUrl());
      final String relativePath = GenerationUtils.toRelativePath(path, moduleBaseDir, m, genOptions);
      jarTag.add(new ZipFileSet(relativePath, "", true));
    }
    final Module module = modules[0];

    //plugin.xml

    final String pluginXmlPath = moduleBuildProperties.getPluginXmlPath();
    final String relativePluginXMLPath = GenerationUtils.toRelativePath(pluginXmlPath, moduleBaseDir, module, genOptions);
    jarTag.add(new ZipFileSet(relativePluginXMLPath, "META-INF/plugin.xml", false));

    //manifest
    final Manifest manifestTag = new Manifest();
    jarTag.add(manifestTag);
    final java.util.jar.Manifest manifest;
    try {
      manifest = PrepareToDeployAction.createOrFindManifest(moduleBuildProperties);
    }
    catch (IOException e) {
      return jarTag;
    }
    manifestTag.applyAttributes(manifest);
    return jarTag;
  }

}