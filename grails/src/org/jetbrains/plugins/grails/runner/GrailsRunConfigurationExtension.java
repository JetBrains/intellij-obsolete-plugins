// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.runner;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NlsContexts.TabTitle;
import com.intellij.openapi.util.Pair;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.groovy.mvc.MvcCommand;

public interface GrailsRunConfigurationExtension<T> {

  @NotNull
  Key<T> getKey();

  default @Nullable SettingsEditor<GrailsRunConfiguration> createExtensionEditor() {
    return null;
  }

  default @Nullable Pair<@TabTitle String, SettingsEditor<GrailsRunConfiguration>> createSettingsEditor(@NotNull Project project) {
    return null;
  }

  @Nullable
  T readAdditionalConfiguration(@NotNull Element element);

  void writeAdditionalConfiguration(@NotNull T cfg, @NotNull Element element);

  @NotNull
  JavaParameters createJavaParameters(@NotNull GrailsApplication grailsApplication,
                                      @NotNull MvcCommand command,
                                      @Nullable T additionalConfiguration) throws ExecutionException;
}
