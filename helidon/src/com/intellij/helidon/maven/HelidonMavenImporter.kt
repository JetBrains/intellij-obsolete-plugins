// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.maven

import com.intellij.helidon.newproject.HelidonRunConfigurationService
import org.jetbrains.idea.maven.importing.MavenAfterImportConfigurator
import org.jetbrains.idea.maven.importing.MavenApplicableConfigurator
import org.jetbrains.idea.maven.importing.hasChanges

internal class HelidonMavenImporter : MavenApplicableConfigurator("io.helidon.build-tools", "helidon-maven-plugin"),
  MavenAfterImportConfigurator {
  override fun afterImport(context: MavenAfterImportConfigurator.Context) {
    val hasApplicableChangedProjects = context.mavenProjectsWithModules.any { it.hasChanges() && isApplicable(it.mavenProject) }
    if (!hasApplicableChangedProjects) return

    context.project.getService(HelidonRunConfigurationService::class.java).createRunConfigurations(context.project)
  }
}