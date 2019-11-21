/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.compiler.ant.packaging;

import com.intellij.compiler.ant.BuildProperties;
import com.intellij.compiler.ant.Generator;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.AntCopyInstructionCreator;
import com.intellij.packaging.elements.ArtifactAntGenerationContext;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.packaging.impl.elements.ArtifactPackagingElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class ArtifactPackagingElementAntGenerator extends PackagingElementAntGenerator<ArtifactPackagingElement> {
  @NotNull
  @Override
  public List<? extends Generator> computeAntInstructions(@NotNull ArtifactPackagingElement packagingElement, @NotNull PackagingElementResolvingContext resolvingContext, @NotNull AntCopyInstructionCreator creator, @NotNull ArtifactAntGenerationContext generationContext, @NotNull ArtifactType artifactType) {
    final Artifact artifact = packagingElement.findArtifact(resolvingContext);
    if (artifact != null) {
      if (artifact.getArtifactType().getSubstitution(artifact, resolvingContext, artifactType) != null) {
        return new ComplexPackagingElementAntGenerator().computeAntInstructions(packagingElement, resolvingContext, creator, generationContext, artifactType);
      }
      final String outputPath = BuildProperties.propertyRef(generationContext.getArtifactOutputProperty(artifact));
      return Collections.singletonList(creator.createDirectoryContentCopyInstruction(outputPath));
    }
    return Collections.emptyList();
  }
}
