// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.grails.gradle;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModelsProvider;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager;

import java.util.Collection;

import static com.intellij.execution.runners.ExecutionUtil.PROPERTY_DYNAMIC_CLASSPATH;

/**
 * @author Vladislav.Soroka
 */
@Order(ExternalSystemConstants.UNORDERED)
public final class GrailsModuleGradleDataService extends AbstractProjectDataService<GrailsModuleData, Module> {

  @Override
  public @NotNull Key<GrailsModuleData> getTargetDataKey() {
    return GrailsModuleData.KEY;
  }

  @Override
  public void importData(final @NotNull Collection<? extends DataNode<GrailsModuleData>> toImport,
                         final @Nullable ProjectData projectData,
                         final @NotNull Project project,
                         final @NotNull IdeModifiableModelsProvider modelsProvider) {
    if (toImport.isEmpty() || !project.isInitialized()) {
      return;
    }

    if (SystemInfo.isWindows) {
      PropertiesComponent.getInstance(project).setValue(PROPERTY_DYNAMIC_CLASSPATH, "true");
    }
  }

  @Override
  public void onSuccessImport(@NotNull Collection<DataNode<GrailsModuleData>> imported,
                              @Nullable ProjectData projectData,
                              @NotNull Project project,
                              @NotNull IdeModelsProvider modelsProvider) {
    GrailsApplicationManager.getInstance(project).queueUpdate();
  }
}
