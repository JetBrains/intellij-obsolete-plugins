/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.compiler.ant.packaging;

import com.intellij.compiler.ant.BuildProperties;
import com.intellij.compiler.ant.Generator;
import com.intellij.gwt.ant.GwtBuildProperties;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.packaging.GwtCompilerOutputElement;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.AntCopyInstructionCreator;
import com.intellij.packaging.elements.ArtifactAntGenerationContext;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class GwtCompilerOutputElementAntGenerator extends PackagingElementAntGenerator<GwtCompilerOutputElement> {
  @NotNull
  @Override
  public List<? extends Generator> computeAntInstructions(@NotNull GwtCompilerOutputElement packagingElement, @NotNull PackagingElementResolvingContext resolvingContext, @NotNull AntCopyInstructionCreator creator, @NotNull ArtifactAntGenerationContext generationContext, @NotNull ArtifactType artifactType) {
    final GwtFacet facet = packagingElement.getFacet();
    if (facet != null) {
      final String outputDir = BuildProperties.propertyRef(GwtBuildProperties.getGwtCompilerOutputPropertyName(facet));
      return Collections.singletonList(creator.createDirectoryContentCopyInstruction(outputDir));
    }
    return Collections.emptyList();
  }
}
