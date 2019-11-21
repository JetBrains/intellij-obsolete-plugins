/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.compiler.ant.packaging;

import com.intellij.packaging.elements.ArtifactAntGenerationContext;
import com.intellij.packaging.impl.elements.ModuleOutputPackagingElementBase;

public class ProductionModuleOutputPackagingElementAntGenerator extends ModuleOutputPackagingElementAntGenerator {
  @Override
  protected String getDirectoryAntProperty(ModuleOutputPackagingElementBase packagingElement, ArtifactAntGenerationContext generationContext) {
    return generationContext.getModuleOutputPath(packagingElement.getModuleName());
  }
}
