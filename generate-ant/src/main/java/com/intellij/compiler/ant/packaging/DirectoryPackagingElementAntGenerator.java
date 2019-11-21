/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.compiler.ant.packaging;

import com.intellij.compiler.ant.Generator;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.AntCopyInstructionCreator;
import com.intellij.packaging.elements.ArtifactAntGenerationContext;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.packaging.impl.elements.DirectoryPackagingElement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DirectoryPackagingElementAntGenerator extends PackagingElementAntGenerator<DirectoryPackagingElement> {
  @NotNull
  @Override
  public List<? extends Generator> computeAntInstructions(@NotNull DirectoryPackagingElement packagingElement, @NotNull PackagingElementResolvingContext resolvingContext, @NotNull AntCopyInstructionCreator creator, @NotNull ArtifactAntGenerationContext generationContext, @NotNull ArtifactType artifactType) {
    final List<Generator> children = new ArrayList<>();
    final Generator command = creator.createSubFolderCommand(packagingElement.getDirectoryName());
    if (command != null) {
      children.add(command);
    }
    children.addAll(computeChildrenGenerators(packagingElement, resolvingContext, creator.subFolder(packagingElement.getDirectoryName()), generationContext, artifactType));
    return children;
  }
}
