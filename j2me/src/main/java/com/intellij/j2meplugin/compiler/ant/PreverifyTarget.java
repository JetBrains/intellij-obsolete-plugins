/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.j2meplugin.compiler.ant;

import com.intellij.compiler.ant.BuildProperties;
import com.intellij.compiler.ant.ModuleChunk;
import com.intellij.compiler.ant.Tag;
import com.intellij.compiler.ant.taskdefs.Target;
import com.intellij.j2meplugin.J2MEBundle;
import com.intellij.j2meplugin.emulator.Emulator;
import com.intellij.j2meplugin.emulator.EmulatorType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkAdditionalData;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.util.PathsList;
import org.jetbrains.annotations.NonNls;

public class PreverifyTarget extends Target {
  public PreverifyTarget(final ModuleChunk chunk) {
    super(J2MEBuildProperties.getPreverifyTargetName(chunk.getName()), null, J2MEBundle.message("ant.preverify.description", chunk.getName()), null);
    final Sdk projectJdk = chunk.getJdk();
    if (projectJdk != null) {
      final SdkAdditionalData additionalData = projectJdk.getSdkAdditionalData();
      if (additionalData instanceof Emulator) {
        final Emulator emulator = (Emulator)additionalData;
        final EmulatorType emulatorType = emulator.getEmulatorType();
        if (emulatorType != null) {
          final String emulatorPreverifyPath = emulatorType.getPreverifyPath();
          if (emulatorPreverifyPath != null) {
            final String preverifyPath = BuildProperties.propertyRef(BuildProperties.getJdkHomeProperty(projectJdk.getName())) + "/" + emulatorPreverifyPath;
            final Tag preverifyTag = new Tag("exec", pair("executable", preverifyPath));
            preverifyTag.add(new Arg("-d " + BuildProperties.propertyRef(BuildProperties.getTempDirForModuleProperty(chunk.getName()))));

            PathsList classpath = new PathsList();
            classpath.add(BuildProperties.propertyRef(BuildProperties.getModuleChunkJdkClasspathProperty(chunk.getName())));
            final Module[] modules = chunk.getModules();
            for (Module module : modules) {
              OrderEnumerator.orderEntries(module).librariesOnly().classes().collectPaths(classpath);
            }
            preverifyTag.add(new Arg("-classpath " + classpath.toString()));

            for (Module module : modules) {
              preverifyTag.add(new Arg(BuildProperties.propertyRef(BuildProperties.getOutputPathProperty(module.getName()))));
            }
            add(preverifyTag);
          }
        }
      }
    }
  }

  private static class Arg extends Tag {
    Arg(@NonNls String value) {
      super("arg", pair("line", value));
    }
  }
}
