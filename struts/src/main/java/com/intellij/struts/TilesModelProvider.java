/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Dmitry Avdeev
 */
public interface TilesModelProvider {

  ExtensionPointName<TilesModelProvider> EP_NAME = ExtensionPointName.create("com.intellij.struts.tilesModelProvider");

  @NotNull
  Collection<TilesModel> computeModels(@NotNull Module module);
}
