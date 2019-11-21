/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.compiler.ant.packaging;

import com.intellij.compiler.ant.Generator;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class PackagingElementAntGenerator<T extends PackagingElement<?>> {
  @NotNull
  public abstract List<? extends Generator> computeAntInstructions(
      @NotNull T packagingElement,
      @NotNull PackagingElementResolvingContext resolvingContext,
      @NotNull AntCopyInstructionCreator creator,
      @NotNull ArtifactAntGenerationContext generationContext,
      @NotNull ArtifactType artifactType);


  protected static List<? extends Generator> computeChildrenGenerators(CompositePackagingElement<?> element,
                                                                       PackagingElementResolvingContext resolvingContext,
                                                                       final AntCopyInstructionCreator copyInstructionCreator,
                                                                       final ArtifactAntGenerationContext generationContext, ArtifactType artifactType) {
    final List<Generator> generators = new ArrayList<>();
    for (PackagingElement<?> child : element.getChildren()) {
      generators.addAll(PackagingElementAntGenerators.computeAntInstructions(child, resolvingContext, copyInstructionCreator, generationContext, artifactType));
    }
    return generators;
  }
}
