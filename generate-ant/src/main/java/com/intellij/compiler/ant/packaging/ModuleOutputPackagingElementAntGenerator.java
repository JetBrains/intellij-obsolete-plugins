/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.compiler.ant.packaging;

import com.intellij.compiler.ant.BuildProperties;
import com.intellij.compiler.ant.Generator;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.AntCopyInstructionCreator;
import com.intellij.packaging.elements.ArtifactAntGenerationContext;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.packaging.impl.elements.ModuleOutputPackagingElementBase;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public abstract class ModuleOutputPackagingElementAntGenerator extends PackagingElementAntGenerator<ModuleOutputPackagingElementBase> {
  @NotNull
  @Override
  public List<? extends Generator> computeAntInstructions(@NotNull ModuleOutputPackagingElementBase packagingElement, @NotNull PackagingElementResolvingContext resolvingContext, @NotNull AntCopyInstructionCreator creator, @NotNull ArtifactAntGenerationContext generationContext, @NotNull ArtifactType artifactType) {
    String property = getDirectoryAntProperty(packagingElement, generationContext);
    if (packagingElement.getModuleName() != null && property != null) {
      final String moduleOutput = BuildProperties.propertyRef(property);
      return Collections.singletonList(creator.createDirectoryContentCopyInstruction(moduleOutput));
    }
    return Collections.emptyList();
  }

  protected abstract String getDirectoryAntProperty(ModuleOutputPackagingElementBase packagingElement, ArtifactAntGenerationContext generationContext);
}
