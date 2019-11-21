/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.compiler.ant.packaging;

import com.intellij.compiler.ant.Generator;
import com.intellij.openapi.util.ClassExtension;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.AntCopyInstructionCreator;
import com.intellij.packaging.elements.ArtifactAntGenerationContext;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class PackagingElementAntGenerators extends ClassExtension<PackagingElementAntGenerator<?>> {
  private PackagingElementAntGenerators() {
    super("com.intellij.generateAnt.packagingElementAntGenerator");
  }

  public static PackagingElementAntGenerators INSTANCE = new PackagingElementAntGenerators();

  public static List<? extends Generator> computeAntInstructions(@NotNull PackagingElement<?> packagingElement,
                                                                 @NotNull PackagingElementResolvingContext resolvingContext,
                                                                 @NotNull AntCopyInstructionCreator creator,
                                                                 @NotNull ArtifactAntGenerationContext generationContext,
                                                                 @NotNull ArtifactType artifactType) {
    PackagingElementAntGenerator generator = INSTANCE.forClass(packagingElement.getClass());
    if (generator == null) {
      return Collections.emptyList();
    }
    return generator.computeAntInstructions(packagingElement, resolvingContext, creator, generationContext, artifactType);
  }
}
