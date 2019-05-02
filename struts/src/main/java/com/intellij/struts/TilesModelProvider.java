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
