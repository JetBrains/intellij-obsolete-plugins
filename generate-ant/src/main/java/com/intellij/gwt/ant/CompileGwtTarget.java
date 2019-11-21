package com.intellij.gwt.ant;

import com.intellij.compiler.ant.BuildProperties;
import com.intellij.compiler.ant.GenerationOptions;
import com.intellij.compiler.ant.taskdefs.AntCall;
import com.intellij.compiler.ant.taskdefs.Param;
import com.intellij.compiler.ant.taskdefs.Property;
import com.intellij.compiler.ant.taskdefs.Target;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.MultiValuesMap;
import org.jetbrains.annotations.NotNull;

/**
 * @author nik
 */
public class CompileGwtTarget extends Target {
  private CompileGwtTarget(@NotNull GwtFacet facet, final String depends, final String description) {
    super(GwtBuildProperties.getCompileGwtTargetName(facet), depends, description, null);
    Project project = facet.getModule().getProject();

    MultiValuesMap<String,GwtFacet> paths = GwtBuildExtension.getGwtSdkPaths(project);
    if (paths.keySet().size() == 1) {
      final String sdkHome = BuildProperties.propertyRef(GwtBuildProperties.getGwtSdkHomeProperty());
      add(new Property(GwtBuildProperties.getGwtSdkHomeProperty(facet), sdkHome));
    }

    GwtModulesManager modulesManager = GwtModulesManager.getInstance(project);
    Module module = facet.getModule();
    for (GwtModule gwtModule : modulesManager.getGwtModuleToCompile(module, true)) {
      AntCall call = new AntCall(GwtBuildProperties.getRunGwtCompilerTargetName(facet));
      call.add(new Param(GwtBuildProperties.getGwtModuleParameter(), gwtModule.getQualifiedName()));
      add(call);
    }
  }

  public static CompileGwtTarget create(@NotNull GwtFacet facet, final GenerationOptions genOptions) {
    String depends = BuildProperties.getCompileTargetName(genOptions.getChunkByModule(facet.getModule()).getName());
    String description = GwtBundle.message("ant.target.description.compile.gwt.modules.in.module.0", facet.getModule().getName());
    return new CompileGwtTarget(facet, depends, description);
  }
}
