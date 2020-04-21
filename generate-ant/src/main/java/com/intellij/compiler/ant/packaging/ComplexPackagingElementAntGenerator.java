/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.compiler.ant.packaging;

import com.intellij.compiler.ant.Generator;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ComplexPackagingElementAntGenerator extends PackagingElementAntGenerator<ComplexPackagingElement<?>> {
  @NotNull
  @Override
  public List<? extends Generator> computeAntInstructions(@NotNull ComplexPackagingElement<?> packagingElement, @NotNull PackagingElementResolvingContext resolvingContext, @NotNull AntCopyInstructionCreator creator, @NotNull ArtifactAntGenerationContext generationContext, @NotNull ArtifactType artifactType) {
    final List<? extends PackagingElement<?>> substitution = packagingElement.getSubstitution(resolvingContext, artifactType);
    if (substitution == null) {
      return Collections.emptyList();
    }

    final List<Generator> fileSets = new ArrayList<>();
    for (PackagingElement<?> element : substitution) {
      fileSets.addAll(PackagingElementAntGenerators.computeAntInstructions(element, resolvingContext, creator, generationContext, artifactType));
    }
    return fileSets;
  }
}
