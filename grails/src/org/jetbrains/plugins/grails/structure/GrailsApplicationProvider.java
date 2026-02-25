// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.structure;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class GrailsApplicationProvider {

  public static final ExtensionPointName<GrailsApplicationProvider> APPLICATION_PROVIDER =
    ExtensionPointName.create("org.intellij.grails.applicationProvider");

  /**
   * Implementations should check if some root corresponds to some Grails application.
   *
   * @param root application root, that is the parent folder of some grails-app.
   * @return instance of Grails application or {@code null}.
   */
  public abstract @Nullable GrailsApplication createApplication(@NotNull Project project, @NotNull VirtualFile root);

  public static @Nullable GrailsApplication createGrailsApplication(@NotNull Project project, @NotNull VirtualFile root) {
    for (GrailsApplicationProvider provider : APPLICATION_PROVIDER.getExtensions()) {
      final GrailsApplication application = provider.createApplication(project, root);
      if (application != null) return application;
    }
    return null;
  }
}
