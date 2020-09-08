package com.intellij.gwt.ant;

import com.intellij.compiler.ant.*;
import com.intellij.compiler.ant.taskdefs.Delete;
import com.intellij.compiler.ant.taskdefs.Property;
import com.intellij.compiler.ant.taskdefs.Target;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.facet.GwtFacetType;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.MultiValuesMap;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.model.impl.sdk.GwtSdkPathUtil;

import java.util.*;

/**
 * @author nik
 */
public class GwtBuildExtension extends ChunkBuildExtension {

  @Override
  @NotNull
  public String[] getTargets(final ModuleChunk chunk) {
    List<String> targets = new ArrayList<>();
    for (Module module : chunk.getModules()) {
      GwtFacet gwtFacet = GwtFacet.getInstance(module);
      if (gwtFacet != null) {
        targets.add(GwtBuildProperties.getCompileGwtTargetName(gwtFacet));
      }
    }
    return ArrayUtilRt.toStringArray(targets);
  }

  @Override
  public void generateProperties(final PropertyFileGenerator generator, final Project project, final GenerationOptions options) {
    MultiValuesMap<String, GwtFacet> gwtSdkPaths = getGwtSdkPaths(project);
    Set<String> paths = gwtSdkPaths.keySet();
    if (paths.size() == 1) {
      generator.addProperty(GwtBuildProperties.getGwtSdkHomeProperty(), paths.iterator().next());
    }
    else {
      for (String path : gwtSdkPaths.keySet()) {
        Collection<GwtFacet> facets = gwtSdkPaths.get(path);
        if (facets != null) {
          for (GwtFacet facet : facets) {
            generator.addProperty(GwtBuildProperties.getGwtSdkHomeProperty(facet), path);
          }
        }
      }
    }

    boolean systemDependentSdkJarUsed = false;
    for (GwtFacet facet : getGwtFacets(project)) {
      systemDependentSdkJarUsed |= !facet.getSdkVersion().isUseSystemIndependentGwtDevJar();
    }
    if (systemDependentSdkJarUsed) {
      generator.addProperty(GwtBuildProperties.getSystemDependentGwtSdkDevJarNameProperty(), GwtSdkPathUtil.getSystemDependentDevJarName());
    }
  }

  public static MultiValuesMap<String, GwtFacet> getGwtSdkPaths(final Project project) {
    MultiValuesMap<String, GwtFacet> gwtSdkPaths = new MultiValuesMap<>(true);
    Module[] modules = ModuleManager.getInstance(project).getModules();
    for (Module module : modules) {
      GwtFacet gwtFacet = GwtFacet.getInstance(module);
      if (gwtFacet != null) {
        gwtSdkPaths.put(VfsUtilCore.urlToPath(gwtFacet.getConfiguration().getSdk().getHomeDirectoryUrl()), gwtFacet);
      }
    }
    return gwtSdkPaths;
  }

  @Override
  public void process(final Project project, final ModuleChunk chunk, final GenerationOptions genOptions, final CompositeGenerator generator) {
    Module[] modules = chunk.getModules();
    for (final Module module : modules) {
      ReadAction.run(() -> {
        GwtFacet facet = GwtFacet.getInstance(module);
        if (facet != null) {
          @NonNls String outputDir = BuildProperties.propertyRef(BuildProperties.getModuleBasedirProperty(facet.getModule())) +
                                     "/GWTCompilerOutput_" + BuildProperties.convertName(facet.getModule().getName());
          generator.add(new Property(GwtBuildProperties.getGwtCompilerOutputPropertyName(facet), outputDir), 1);
          Comment comment = new Comment(
              "Run GWT compiler for GWT module " +
                                BuildProperties.propertyRef(GwtBuildProperties.getGwtModuleParameter()));
          generator.add(comment, 1);
          generator.add(new RunGwtCompilerTarget(facet, genOptions));
          generator.add(CompileGwtTarget.create(facet, genOptions), 1);
        }
      });
    }
  }

  @Override
  public void generateProjectTargets(Project project, GenerationOptions genOptions, CompositeGenerator generator) {
    if (!getGwtFacets(project).isEmpty()) {
      final List<GwtFacet> facets = getGwtFacets(project);
      final Target target = new Target(GwtBuildProperties.getGwtCleanTargetName(), null, "Clean GWT Compiler output directories", null);
      for (GwtFacet facet : facets) {
        target.add(new Delete(BuildProperties.propertyRef(GwtBuildProperties.getGwtCompilerOutputPropertyName(facet))));
      }
      generator.add(target, 1);
    }
  }

  private static List<GwtFacet> getGwtFacets(Project project) {
    return ProjectFacetManager.getInstance(project).getFacets(GwtFacetType.ID);
  }

  @Override
  public List<String> getCleanTargetNames(Project project, GenerationOptions genOptions) {
    if (!getGwtFacets(project).isEmpty()) {
      return Collections.singletonList(GwtBuildProperties.getGwtCleanTargetName());
    }
    return Collections.emptyList();
  }
}
