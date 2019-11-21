/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.compiler.ant.packaging;

import com.intellij.compiler.ant.BuildProperties;
import com.intellij.compiler.ant.Generator;
import com.intellij.compiler.ant.taskdefs.Include;
import com.intellij.compiler.ant.taskdefs.Mkdir;
import com.intellij.compiler.ant.taskdefs.PatternSet;
import com.intellij.compiler.ant.taskdefs.Unzip;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.AntCopyInstructionCreator;
import com.intellij.packaging.elements.ArtifactAntGenerationContext;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.packaging.impl.elements.ExtractedDirectoryPackagingElement;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class ExtractedDirectoryPackagingElementAntGenerator extends PackagingElementAntGenerator<ExtractedDirectoryPackagingElement> {
  @NotNull
  @Override
  public List<? extends Generator> computeAntInstructions(@NotNull ExtractedDirectoryPackagingElement packagingElement, @NotNull PackagingElementResolvingContext resolvingContext, @NotNull AntCopyInstructionCreator creator, @NotNull ArtifactAntGenerationContext generationContext, @NotNull ArtifactType artifactType) {
    String filePath = packagingElement.getFilePath();
    final String jarPath = generationContext.getSubstitutedPath(filePath);
    final String pathInJar = StringUtil.trimStart(packagingElement.getPathInJar(), "/");
    if (pathInJar.length() == 0) {
      return Collections.singletonList(creator.createExtractedDirectoryInstruction(jarPath));
    }

    final String archiveName = PathUtil.getFileName(filePath);
    final String tempDirProperty = generationContext.createNewTempFileProperty("temp.unpacked.path." + archiveName, archiveName);
    final String tempDirPath = BuildProperties.propertyRef(tempDirProperty);
    generationContext.runBeforeCurrentArtifact(new Mkdir(tempDirPath));

    final Unzip unzip = new Unzip(jarPath, tempDirPath);
    final PatternSet patterns = new PatternSet(null);
    patterns.add(new Include(pathInJar + "**"));
    unzip.add(patterns);
    generationContext.runBeforeCurrentArtifact(unzip);

    return Collections.singletonList(creator.createDirectoryContentCopyInstruction(tempDirPath + "/" + pathInJar));
  }
}
