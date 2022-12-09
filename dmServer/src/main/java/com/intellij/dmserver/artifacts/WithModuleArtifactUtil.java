package com.intellij.dmserver.artifacts;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class WithModuleArtifactUtil {

  public static Artifact findDmBundleArtifactFor(@NotNull Module module) {
    Project project = module.getProject();
    ArtifactManager manager = ArtifactManager.getInstance(project);
    List<Artifact> scope = new ArrayList<>();
    scope.addAll(manager.getArtifactsByType(DMBundleArtifactType.getInstance()));
    scope.addAll(manager.getArtifactsByType(DMParArtifactType.getInstance()));
    scope.addAll(manager.getArtifactsByType(DMPlanArtifactType.getInstance()));
    scope.addAll(manager.getArtifactsByType(DMConfigArtifactType.getInstance()));
    for (Artifact nextArtifact : scope) {
      Module nextModule = findModuleFor(project, nextArtifact);
      if (module.equals(nextModule)) {
        return nextArtifact;
      }
    }
    return null;
  }

  @NotNull
  public static List<Artifact> findWithModuleArtifactsFor(@NotNull Module module) {
    Project project = module.getProject();
    List<Artifact> result = new ArrayList<>();
    for (Artifact next : ArtifactManager.getInstance(project).getArtifacts()) {
      Module nextModule = findModuleFor(project, next);
      if (module.equals(nextModule)) {
        result.add(next);
      }
    }
    return result;
  }

  @NotNull
  public static List<Artifact> findWithModuleArtifactsFor(Project project, @NotNull String moduleName) {
    List<Artifact> result = new ArrayList<>();
    for (Artifact next : ArtifactManager.getInstance(project).getArtifacts()) {
      String nextModuleName = findModuleNameFor(project, next);
      if (moduleName.equals(nextModuleName)) {
        result.add(next);
      }
    }
    return result;
  }

  @Nullable
  private static String findModuleNameFor(Project project, Artifact artifact) {
    if (!(artifact.getArtifactType() instanceof DMArtifactTypeBase)) {
      return null;
    }
    DMArtifactTypeBase<?, ?, ?> artifactType = (DMArtifactTypeBase<?, ?, ?>)artifact.getArtifactType();

    final List<String> moduleNames = new ArrayList<>();
    Processor<WithModulePackagingElement> processor = element -> {
      String moduleName = element.getModuleName();
      if (moduleName != null) {
        moduleNames.add(moduleName);
      }
      return false;
    };
    final PackagingElementResolvingContext context = ArtifactManager.getInstance(project).getResolvingContext();
    ArtifactUtil.processPackagingElements(artifact, artifactType.getModulePackagingElementType(), processor, context, false);
    if (moduleNames.isEmpty()) {
      return null;
    }
    return moduleNames.get(0);
  }

  @Nullable
  public static Module findModuleFor(Project project, Artifact artifact) {
    String moduleName = findModuleNameFor(project, artifact);
    return moduleName == null ? null : ModuleManager.getInstance(project).findModuleByName(moduleName);
  }
}
