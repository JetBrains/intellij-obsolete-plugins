/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.j2meplugin.compiler.ant;

import com.intellij.compiler.ant.ChunkBuildExtension;
import com.intellij.compiler.ant.CompositeGenerator;
import com.intellij.compiler.ant.GenerationOptions;
import com.intellij.compiler.ant.ModuleChunk;
import com.intellij.j2meplugin.module.J2MEModuleType;
import com.intellij.j2meplugin.module.settings.MobileModuleSettings;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class ChunkBuildJ2MEExtension extends ChunkBuildExtension {

  @Override
  @NotNull
  @NonNls
  public String[] getTargets(final ModuleChunk chunk) {
    return isME(chunk.getModules()) ? new String[]{J2MEBuildProperties.getMobileBuildTargetName(chunk.getName())}
           : ArrayUtil.EMPTY_STRING_ARRAY;
  }

  @Override
  public void process(Project project, ModuleChunk chunk, GenerationOptions genOptions, CompositeGenerator generator) {
    final Module[] modules = chunk.getModules();
    if (isME(modules)) {
      final Module module = modules[0];

      generator.add(new CompositeJ2METarget(chunk, genOptions), 1);
      generator.add(new PreverifyTarget(chunk), 1);

      generator.add(new BuildJarTarget(chunk, genOptions, MobileModuleSettings.getInstance(module)), 1);
    }
  }

  private static boolean isME(Module[] modules) {
    return modules.length == 1 && ModuleType.get(modules[0]) == J2MEModuleType.getInstance();
  }
}