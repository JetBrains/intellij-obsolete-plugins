/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.compiler.ant.packaging;

import com.intellij.compiler.ant.BuildProperties;
import com.intellij.compiler.ant.Generator;
import com.intellij.javaee.ui.packaging.JavaeeFacetClassesPackagingElement;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.AntCopyInstructionCreator;
import com.intellij.packaging.elements.ArtifactAntGenerationContext;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class JavaeeFacetClassesPackagingElementAntGenerator extends PackagingElementAntGenerator<JavaeeFacetClassesPackagingElement> {
  @NotNull
  @Override
  public List<? extends Generator> computeAntInstructions(@NotNull JavaeeFacetClassesPackagingElement packagingElement, @NotNull PackagingElementResolvingContext resolvingContext, @NotNull AntCopyInstructionCreator creator, @NotNull ArtifactAntGenerationContext generationContext, @NotNull ArtifactType artifactType) {
    final String moduleOutput = BuildProperties.propertyRef(generationContext.getModuleOutputPath(packagingElement.getModuleName()));
    return Collections.singletonList(creator.createDirectoryContentCopyInstruction(moduleOutput));
  }
}
