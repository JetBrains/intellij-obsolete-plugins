/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.compiler.ant;

import com.intellij.compiler.ant.artifacts.ArtifactsGenerator;
import com.intellij.compiler.ant.taskdefs.AntProject;
import com.intellij.compiler.ant.taskdefs.Target;
import com.intellij.openapi.project.Project;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * @author Eugene Zhuravlev
 */
public abstract class ProjectBuild extends Generator {
  protected final Project myProject;
  private final AntProject myAntProject;

  public ProjectBuild(Project project, GenerationOptions genOptions) {
    myProject = project;
    myAntProject = new AntProject(BuildProperties.getProjectBuildFileName(myProject), BuildProperties.DEFAULT_TARGET);

    myAntProject.add(new BuildPropertiesImpl(myProject, genOptions), 1);

    // the sequence in which modules are imported is important cause output path properties for dependent modules should be defined first

    final StringBuilder buildModulesTargetNames = new StringBuilder();
    buildModulesTargetNames.append(BuildProperties.TARGET_INIT);
    buildModulesTargetNames.append(", ");
    buildModulesTargetNames.append(BuildProperties.TARGET_CLEAN);
    final ModuleChunk[] chunks = genOptions.getModuleChunks();

    if (chunks.length > 0) {
      myAntProject.add(new Comment(GenerateAntBundle.message("generated.ant.build.modules.section.title")), 1);

      for (final ModuleChunk chunk : chunks) {
        myAntProject.add(createModuleBuildGenerator(chunk, genOptions), 1);
        final String[] targets = ChunkBuildExtension.getAllTargets(chunk);
        for (String target : targets) {
          if (buildModulesTargetNames.length() > 0) {
            buildModulesTargetNames.append(", ");
          }
          buildModulesTargetNames.append(target);
        }
      }
    }
    for (ChunkBuildExtension extension : ChunkBuildExtension.EP_NAME.getExtensions()) {
      extension.generateProjectTargets(project, genOptions, myAntProject);
    }

    final Target initTarget = new Target(BuildProperties.TARGET_INIT, null,
                                         GenerateAntBundle.message("generated.ant.build.initialization.section.title"), null);
    initTarget.add(new Comment(GenerateAntBundle.message("generated.ant.build.initialization.section.comment")));
    myAntProject.add(initTarget, 1);

    ArtifactsGenerator artifactsGenerator = new ArtifactsGenerator(project, genOptions);

    myAntProject.add(new CleanProject(project, genOptions, artifactsGenerator), 1);

    myAntProject.add(new Target(BuildProperties.TARGET_BUILD_MODULES, buildModulesTargetNames.toString(),
                                GenerateAntBundle.message("generated.ant.build.build.all.modules.target.name"), null), 1);

    StringBuilder buildAllTargetNames = new StringBuilder();
    buildAllTargetNames.append(BuildProperties.TARGET_BUILD_MODULES);
    if (artifactsGenerator.hasArtifacts()) {
      List<Generator> generators = artifactsGenerator.generate();
      for (Generator generator : generators) {
        myAntProject.add(generator, 1);
      }

      buildAllTargetNames.append(", ").append(ArtifactsGenerator.BUILD_ALL_ARTIFACTS_TARGET);
    }

    myAntProject.add(new Target(BuildProperties.TARGET_ALL, buildAllTargetNames.toString(),
                                GenerateAntBundle.message("generated.ant.build.build.all.target.name"), null), 1);
  }

  @Override
  public void generate(PrintWriter out) throws IOException {
    writeXmlHeader(out);
    myAntProject.generate(out);
  }

  protected abstract Generator createModuleBuildGenerator(final ModuleChunk chunk, GenerationOptions genOptions);

}
