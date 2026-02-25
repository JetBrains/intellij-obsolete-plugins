// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.grails.gradle;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.SimpleJavaParameters;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.util.PathUtil;
import org.gradle.tooling.model.idea.IdeaModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.groovy.grails.rt.GrailsRtMarker;
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.grails.gradle.tooling.builder.GrailsModule;
import org.jetbrains.plugins.grails.gradle.tooling.builder.GrailsModuleModelBuilderImpl;

import java.util.Collections;
import java.util.Set;

/**
 * @author Vladislav.Soroka
 */
@Order(ExternalSystemConstants.UNORDERED)
public final class GrailsProjectResolverExtension extends AbstractProjectResolverExtension {

  @Override
  public void populateModuleExtraModels(@NotNull IdeaModule gradleModule, @NotNull DataNode<ModuleData> ideModule) {
    GrailsModule grailsModule = resolverCtx.getExtraProject(gradleModule, GrailsModule.class);
    if (grailsModule != null) {
      ideModule.createChild(GrailsModuleData.KEY, new GrailsModuleData(
        GradleConstants.SYSTEM_ID,
        grailsModule.getGrailsVersion(),
        grailsModule.getGrailsPluginId(),
        grailsModule.getShellUrls()
      ));
    }

    nextResolver.populateModuleExtraModels(gradleModule, ideModule);
  }

  @Override
  public @NotNull Set<Class<?>> getExtraProjectModelClasses() {
    return Collections.singleton(GrailsModule.class);
  }

  @Override
  public @NotNull Set<Class<?>> getToolingExtensionsClasses() {
    return Set.of(
      // grails-gradle-tooling jar
      GrailsModuleModelBuilderImpl.class,
      GrailsRtMarker.class
    );
  }

  @Override
  public void enhanceRemoteProcessing(@NotNull SimpleJavaParameters parameters) throws ExecutionException {
    parameters.getClassPath().add(PathUtil.getJarPathForClass(GrailsRtMarker.class));
  }
}
