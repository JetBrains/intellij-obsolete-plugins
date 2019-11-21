/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.compiler.ant;

import com.intellij.compiler.ant.taskdefs.Delete;
import com.intellij.compiler.ant.taskdefs.Target;

/**
 * @author Eugene Zhuravlev
 */
public class CleanModule extends Target {
  public CleanModule(ModuleChunk chunk) {
    super(BuildProperties.getModuleCleanTargetName(chunk.getName()), null,
          GenerateAntBundle.message("generated.ant.build.cleanup.module.task.comment"), null);
    final String chunkName = chunk.getName();
    add(new Delete(BuildProperties.propertyRef(BuildProperties.getOutputPathProperty(chunkName))));
    add(new Delete(BuildProperties.propertyRef(BuildProperties.getOutputPathForTestsProperty(chunkName))));
  }
}
