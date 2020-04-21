/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.compiler.ant.packaging;

import com.intellij.compiler.ant.Generator;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.AntCopyInstructionCreator;
import com.intellij.packaging.elements.ArtifactAntGenerationContext;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import org.jetbrains.android.compiler.artifact.AndroidFinalPackageElement;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.facet.AndroidRootUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class AndroidFinalPackageElementAntGenerator extends PackagingElementAntGenerator<AndroidFinalPackageElement> {
  @NotNull
  @Override
  public List<? extends Generator> computeAntInstructions(@NotNull AndroidFinalPackageElement packagingElement, @NotNull PackagingElementResolvingContext resolvingContext, @NotNull AntCopyInstructionCreator creator, @NotNull ArtifactAntGenerationContext generationContext, @NotNull ArtifactType artifactType) {
    final String apkPath = getApkPath(packagingElement);
    if (apkPath != null) {
      return Collections.singletonList(creator.createExtractedDirectoryInstruction(apkPath));
    }
    return Collections.emptyList();
  }

  @Nullable
  private String getApkPath(AndroidFinalPackageElement packagingElement) {
    AndroidFacet facet = packagingElement.getFacet();
    if (facet == null) {
      return null;
    }

    final String apkPath = AndroidRootUtil.getApkPath(facet);
    final String path = apkPath != null
        ? addSuffixToFileName(apkPath, ".afp")
        : null;
    return path != null
        ? FileUtil.toSystemIndependentName(path) + "!/"
        : null;
  }

  @NotNull
  private static String addSuffixToFileName(@NotNull String path, @NotNull String suffix) {
    int dot = path.lastIndexOf('.');
    if (dot < 0) {
      return path + suffix;
    }
    String a = path.substring(0, dot);
    String b = path.substring(dot);
    return a + suffix + b;
  }
}


