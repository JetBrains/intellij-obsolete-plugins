package com.intellij.gwt.build;

import com.intellij.compiler.impl.BuildTargetScopeProvider;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.make.report.CompileReportSource;
import com.intellij.gwt.packaging.GwtCompilerOutputElementType;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.packaging.impl.compiler.ArtifactCompileScope;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.builders.java.JavaModuleBuildTargetType;
import org.jetbrains.jps.gwt.build.GwtBuildTargetType;
import org.jetbrains.jps.gwt.build.GwtBuilderParameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jps.api.CmdlineRemoteProto.Message.ControllerMessage.ParametersMessage.TargetTypeBuildScope;

public final class GwtBuildTargetScopeProvider extends BuildTargetScopeProvider {
  private static final Key<CompileReportSource> COMPILE_REPORT_SOURCE_KEY = Key.create(GwtBuilderParameters.GWT_COMPILE_REPORT_PARAMETER);

  public static Set<GwtFacet> getFacetsToBuild(final Project project, final CompileScope compileScope) {
    final Set<Artifact> artifacts = ArtifactCompileScope.getArtifactsToBuild(project, compileScope, false);
    return getFacetsIncludedInArtifacts(artifacts, project);
  }

  public static @Nullable CompileReportSource getCompileReportSource(final CompileScope scope) {
    return scope.getUserData(COMPILE_REPORT_SOURCE_KEY);
  }

  public static void setGenerateCompileReportOption(@NotNull CompileScope scope, @NotNull CompileReportSource source) {
    scope.putUserData(COMPILE_REPORT_SOURCE_KEY, source);
  }

  private static Set<GwtFacet> getFacetsIncludedInArtifacts(final Collection<? extends Artifact> artifacts, final Project project) {
    final Set<GwtFacet> facets = new HashSet<>();
    ReadAction.runBlocking(() -> {
      for (Artifact artifact : artifacts) {
        final PackagingElementResolvingContext context = ArtifactManager.getInstance(project).getResolvingContext();
        ArtifactUtil.processPackagingElements(artifact, GwtCompilerOutputElementType.getInstance(),
                                              element -> {
                                                ContainerUtil.addIfNotNull(facets, element.getFacet());
                                                return true;
                                              }, context, true);
      }
    });
    return facets;
  }

  @Override
  public @NotNull List<TargetTypeBuildScope> getBuildTargetScopes(final @NotNull CompileScope baseScope,
                                                                  final @NotNull Project project, boolean forceBuild) {
    Set<GwtFacet> facets = ReadAction.computeBlocking(() -> {
      Set<GwtFacet> gwtFacets = getFacetsToBuild(project, baseScope);
      CompileReportSource reportSource = getCompileReportSource(baseScope);
      if (reportSource != null) {
        gwtFacets.add(reportSource.getFacet());
      }
      return gwtFacets;
    });
    if (!facets.isEmpty()) {
      List<TargetTypeBuildScope> scopes = new ArrayList<>();
      TargetTypeBuildScope.Builder gwtScope = TargetTypeBuildScope.newBuilder().setTypeId(GwtBuildTargetType.TYPE_ID)
        .setForceBuild(ArtifactCompileScope.isArtifactRebuildForced(baseScope));
      TargetTypeBuildScope.Builder modulesScope = TargetTypeBuildScope.newBuilder().setTypeId(JavaModuleBuildTargetType.PRODUCTION.getTypeId()).setForceBuild(forceBuild);
      for (GwtFacet facet : facets) {
        String moduleName = facet.getModule().getName();
        gwtScope.addTargetId(moduleName);
        modulesScope.addTargetId(moduleName);
      }
      scopes.add(gwtScope.build());
      scopes.add(modulesScope.build());
      return scopes;
    }
    return Collections.emptyList();
  }
}
