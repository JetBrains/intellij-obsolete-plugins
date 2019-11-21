/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.compiler.ant.packaging;

import com.intellij.compiler.ant.BuildProperties;
import com.intellij.compiler.ant.Generator;
import com.intellij.compiler.ant.Tag;
import com.intellij.compiler.ant.artifacts.ArchiveAntCopyInstructionCreator;
import com.intellij.compiler.ant.taskdefs.Jar;
import com.intellij.compiler.ant.taskdefs.Zip;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.AntCopyInstructionCreator;
import com.intellij.packaging.elements.ArtifactAntGenerationContext;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.packaging.impl.elements.ArchivePackagingElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class ArchivePackagingElementAntGenerator extends PackagingElementAntGenerator<ArchivePackagingElement> {
  @NotNull
  @Override
  public List<? extends Generator> computeAntInstructions(@NotNull ArchivePackagingElement packagingElement, @NotNull PackagingElementResolvingContext resolvingContext, @NotNull AntCopyInstructionCreator creator, @NotNull ArtifactAntGenerationContext generationContext, @NotNull ArtifactType artifactType) {
    String archiveFileName = packagingElement.getArchiveFileName();
    final String tempJarProperty = generationContext.createNewTempFileProperty("temp.jar.path." + archiveFileName, archiveFileName);
    String jarPath = BuildProperties.propertyRef(tempJarProperty);
    final Tag jar;
    if (archiveFileName.endsWith(".jar")) {
      jar = new Jar(jarPath, "preserve", true);
    } else {
      jar = new Zip(jarPath);
    }
    for (Generator generator : computeChildrenGenerators(packagingElement, resolvingContext, new ArchiveAntCopyInstructionCreator(""), generationContext, artifactType)) {
      jar.add(generator);
    }
    generationContext.runBeforeCurrentArtifact(jar);
    return Collections.singletonList(creator.createFileCopyInstruction(jarPath, archiveFileName));
  }
}
