/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.compiler.ant;

import com.intellij.compiler.ant.artifacts.ArtifactsGenerator;
import com.intellij.compiler.ant.taskdefs.Target;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene Zhuravlev
 */
public class CleanProject extends Generator {
  private final Target myTarget;

  public CleanProject(Project project, @NotNull GenerationOptions genOptions, @NotNull ArtifactsGenerator artifactsGenerator) {
    List<String> dependencies = new ArrayList<>();
    final ModuleChunk[] chunks = genOptions.getModuleChunks();
    for (ModuleChunk chunk : chunks) {
      dependencies.add(BuildProperties.getModuleCleanTargetName(chunk.getName()));
    }
    dependencies.addAll(artifactsGenerator.getCleanTargetNames());
    for (ChunkBuildExtension extension : ChunkBuildExtension.EP_NAME.getExtensions()) {
      dependencies.addAll(extension.getCleanTargetNames(project, genOptions));
    }
    myTarget = new Target(BuildProperties.TARGET_CLEAN, StringUtil.join(dependencies, ", "),
        GenerateAntBundle.message("generated.ant.build.clean.all.task.comment"), null);
  }

  @Override
  public void generate(PrintWriter out) throws IOException {
    myTarget.generate(out);
  }
}
