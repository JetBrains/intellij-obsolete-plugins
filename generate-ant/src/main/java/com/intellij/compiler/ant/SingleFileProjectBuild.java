/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.compiler.ant;

import com.intellij.compiler.ant.taskdefs.Dirname;
import com.intellij.openapi.project.Project;

/**
 * @author Eugene Zhuravlev
 */
public class SingleFileProjectBuild extends ProjectBuild {
  public SingleFileProjectBuild(Project project, GenerationOptions genOptions) {
    super(project, genOptions);
  }

  @Override
  protected Generator createModuleBuildGenerator(ModuleChunk chunk, GenerationOptions genOptions) {
    final CompositeGenerator gen = new CompositeGenerator();
    gen.add(new Comment(GenerateAntBundle.message("generated.ant.build.building.concrete.module.section.title", chunk.getName())));
    gen.add(new Dirname(BuildProperties.getModuleChunkBasedirProperty(chunk), BuildProperties.propertyRef("ant.file")), 1);
    gen.add(new ChunkBuild(myProject, chunk, genOptions), 1);
    return gen;
  }

}
