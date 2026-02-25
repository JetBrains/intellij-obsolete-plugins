// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.grails.gradle;

import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData;
import com.intellij.serialization.PropertyMapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public final class GrailsModuleData extends AbstractExternalEntityData {
  public static final @NotNull Key<GrailsModuleData> KEY =
    Key.create(GrailsModuleData.class, ProjectKeys.LIBRARY_DEPENDENCY.getProcessingWeight() + 1);

  private final @NotNull String grailsVersion;

  private final @NotNull String grailsPluginId;

  private final @Nullable List<String> shellUrls;

  @PropertyMapping({"owner", "grailsVersion", "grailsPluginId", "shellUrls"})
  public GrailsModuleData(@NotNull ProjectSystemId owner,
                          @NotNull String grailsVersion,
                          @NotNull String grailsPluginId,
                          @Nullable List<String> shellUrls) {
    super(owner);
    this.grailsVersion = grailsVersion;
    this.grailsPluginId = grailsPluginId;
    this.shellUrls = shellUrls;
  }

  public @NotNull String getGrailsVersion() {
    return grailsVersion;
  }

  public @NotNull String getGrailsPluginId() {
    return grailsPluginId;
  }

  public @NotNull List<String> getShellUrls() {
    return shellUrls == null ? Collections.emptyList() : shellUrls;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    GrailsModuleData data = (GrailsModuleData)o;

    if (!grailsVersion.equals(data.grailsVersion)) return false;
    if (!grailsPluginId.equals(data.grailsPluginId)) return false;
    if (shellUrls != null ? !shellUrls.equals(data.shellUrls) : data.shellUrls != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + grailsVersion.hashCode();
    result = 31 * result + grailsPluginId.hashCode();
    result = 31 * result + (shellUrls != null ? shellUrls.hashCode() : 0);
    return result;
  }
}
