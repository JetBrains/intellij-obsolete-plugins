// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.config;

import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.extensions.GroovyUnresolvedHighlightFileFilter;

public final class GrailsConfigUnresolvedFileFilter extends GroovyUnresolvedHighlightFileFilter {
  @Override
  public boolean isReject(@NotNull PsiFile file) {
    return GrailsUtils.isBuildConfigFile(file) || GrailsUtils.isConfigFile(file, "Config.groovy") || GrailsUtils.isConfigFile(file, "DataSource.groovy");
  }
}
