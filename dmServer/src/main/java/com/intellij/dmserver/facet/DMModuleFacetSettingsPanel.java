package com.intellij.dmserver.facet;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public interface DMModuleFacetSettingsPanel<C extends DMFacetConfigurationBase<C>> {

  void init(@Nullable Project project,
            @Nullable Module configuredModule,
            @NotNull ModulesProvider modulesProvider,
            @NotNull Disposable parentDisposable);

  @NotNull
  JPanel getMainPanel();

  void apply(@NotNull C configuration);

  void save(@NotNull C configuration);

  void load(@NotNull C configuration);

  void updateEnablement();
}
