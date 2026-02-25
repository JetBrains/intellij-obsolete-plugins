// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.groovy.grails.maven;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.plugins.grails.structure.impl.OldGrailsModuleBasedApplication;

final class GrailsMavenApplication extends OldGrailsModuleBasedApplication {

  private final @NotNull MavenId myMavenId;

  GrailsMavenApplication(@NotNull Module module, @NotNull VirtualFile root, @NotNull MavenId id) {
    super(module, root);
    myMavenId = id;
  }

  @Override
  public @NotNull String getName() {
    return StringUtil.notNullize(myMavenId.getArtifactId());
  }

  @Override
  public @Nullable String getAppVersion() {
    return myMavenId.getVersion();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    GrailsMavenApplication that = (GrailsMavenApplication)o;

    return myMavenId.equals(that.myMavenId);
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + myMavenId.hashCode();
  }
}
