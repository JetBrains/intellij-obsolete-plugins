/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.j2meplugin.compiler.ant;

import com.intellij.compiler.ant.*;
import com.intellij.compiler.ant.taskdefs.*;
import com.intellij.j2meplugin.J2MEBundle;
import com.intellij.j2meplugin.module.settings.MobileApplicationType;
import com.intellij.j2meplugin.module.settings.MobileModuleSettings;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.io.FileUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class BuildJarTarget extends Target {
  public BuildJarTarget(ModuleChunk chunk, final GenerationOptions genOptions, final MobileModuleSettings settings) {
    super(J2MEBuildProperties.getJarBuildTargetName(chunk.getName()), null,
          J2MEBundle.message("ant.jar.description", chunk.getName()), null);
    final Module[] modules = chunk.getModules();
    final Module module = modules[0];
    final String moduleName = module.getName();


    final MobileApplicationType mobileApplicationType = settings.getApplicationType();
    
    final String destFile = GenerationUtils.toRelativePath(settings.getJarURL(), chunk, genOptions);

    final String jarPathProperty = J2MEBuildProperties.getJarPathProperty();

    add(new Property(jarPathProperty, destFile));

    final String jarPathPropertyRef = BuildProperties.propertyRef(jarPathProperty);

    final Jar jarTag = new Jar(jarPathPropertyRef, "preserve");

    jarTag.add(new ZipFileSet(BuildProperties.propertyRef(BuildProperties.getTempDirForModuleProperty(moduleName)), "", true));
    createManifest(jarTag, settings, chunk, genOptions);
    add(jarTag);

    add(new Tag("length", pair("file", jarPathPropertyRef), pair("property", J2MEBuildProperties.getJarSizeProperty())));
    final String jadProperty = J2MEBuildProperties.getExtensionPathProperty(mobileApplicationType);
    add(new Property(jadProperty, GenerationUtils.toRelativePath(settings.getMobileDescriptionPath(), chunk, genOptions)));
    add(new Tag("replaceregexp",
                pair("file", BuildProperties.propertyRef(jadProperty)),
                pair("match", mobileApplicationType.getJarSizeSettingName() + mobileApplicationType.getSeparator() + " .*"),
                pair("replace", mobileApplicationType.getJarSizeSettingName() + mobileApplicationType.getSeparator() + " " + BuildProperties.propertyRef(J2MEBuildProperties.getJarSizeProperty())),
                pair("byline", "true")));
  }

  private void createManifest(final Jar jarTag,
                              final MobileModuleSettings settings,
                              final ModuleChunk chunk,
                              final GenerationOptions genOptions) {
    final Manifest manifestTag = new Manifest();
    jarTag.add(manifestTag);
    final File manifestFile;
    final String userManifestPath = settings.getUserManifestPath();
    if (settings.isUseUserManifest() && userManifestPath != null) {
      add(
        new Property(J2MEBuildProperties.getManifestPath(), GenerationUtils.toRelativePath(userManifestPath, chunk, genOptions)));
      manifestFile = new File(userManifestPath);
    }
    else {
      manifestFile = settings.getManifest();
    }
    final Properties properties = new Properties();
    try {
      InputStream is = new ByteArrayInputStream(FileUtil.loadFileBytes(manifestFile));
      properties.load(is);
      is.close();
    }
    catch (IOException e) {
      return;
    }

    //set up idea attributes
    manifestTag.applyAttributes(new java.util.jar.Manifest());

    //all other essential options
    for (Object key : properties.keySet()) {
      manifestTag.add(new Attribute((String)key, properties.getProperty((String)key)));
    }
  }

}