// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.bigdatatools.plugin.spark.services.actions;

import com.intellij.bigdatatools.plugin.spark.services.SparkJobServiceViewContributor;
import com.intellij.execution.services.ServiceViewAddActionContributor;
import com.intellij.ide.actions.NonEmptyActionGroup;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.jetbrains.bigdatatools.common.constants.BdtPlugins;
import org.jetbrains.annotations.NotNull;

final class AddSparkServiceActionGroup extends NonEmptyActionGroup
  implements DumbAware, ServiceViewAddActionContributor {

  @Override
  public @NotNull Class<?> getContributorClass() {
    return SparkJobServiceViewContributor.class;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(e.getProject() != null && BdtPlugins.INSTANCE.isSparkPluginInstalled());
  }
}
