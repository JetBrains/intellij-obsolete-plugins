/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.j2meplugin.compiler.ant;

import com.intellij.compiler.ant.*;
import com.intellij.compiler.ant.taskdefs.*;
import com.intellij.j2meplugin.J2MEBundle;
import com.intellij.j2meplugin.compiler.MobileMakeUtil;
import com.intellij.j2meplugin.module.J2MEModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;

import java.io.File;
import java.util.HashSet;

public class CompositeJ2METarget extends CompositeGenerator {

  public CompositeJ2METarget(final ModuleChunk chunk, final GenerationOptions genOptions) {
    final File moduleBaseDir = chunk.getBaseDir();
    final Module module = chunk.getModules()[0];
    final String moduleName = module.getName();
    final Target buildTarget = new Target(J2MEBuildProperties.getMobileBuildTargetName(moduleName), getDepends(module),
                                          J2MEBundle.message("ant.suite.description", chunk.getName()), null);

    final File jarDir = new File(moduleBaseDir.getParentFile(), "temp");
    String tempDir = GenerationUtils.toRelativePath(jarDir.getPath(), chunk, genOptions);
    final String tempDirProperty = BuildProperties.getTempDirForModuleProperty(moduleName);

    buildTarget.add(new Property(tempDirProperty, tempDir));
    buildTarget.add(new Mkdir(BuildProperties.propertyRef(tempDirProperty)));

    AntCall preverifyCall = new AntCall(J2MEBuildProperties.getPreverifyTargetName(moduleName));
    buildTarget.add(preverifyCall);

    AntCall jarCall = new AntCall(J2MEBuildProperties.getJarBuildTargetName(moduleName));
    buildTarget.add(jarCall);

    buildTarget.add(new Delete(BuildProperties.propertyRef(tempDirProperty)));
    add(buildTarget);
  }

  private static String getDepends(final Module module) {
    final HashSet<Module> modules = new HashSet<>();
    MobileMakeUtil.getDependencies(module, modules);
    
    String depends = BuildProperties.getCompileTargetName(module.getName());

    for (Module dependentModule : modules) {
      final String name = dependentModule.getName();
      if (ModuleType.get(dependentModule) == J2MEModuleType.getInstance()) {
        depends += ", " + J2MEBuildProperties.getMobileBuildTargetName(name);
      }
      else {
        depends += ", " + BuildProperties.getCompileTargetName(name);
      }

    }
    return depends;

  }
}