package com.intellij.gwt.ant;

import com.intellij.compiler.ant.BuildProperties;
import com.intellij.compiler.ant.GenerationOptions;
import com.intellij.compiler.ant.LibraryDefinitionsGeneratorFactory;
import com.intellij.compiler.ant.Tag;
import com.intellij.compiler.ant.taskdefs.PathElement;
import com.intellij.compiler.ant.taskdefs.PathRef;
import com.intellij.compiler.ant.taskdefs.Target;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.facet.GwtFacetConfiguration;
import com.intellij.gwt.run.GwtClasspathUtil;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.model.impl.sdk.GwtSdkPathUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author nik
 */
public class RunGwtCompilerTarget extends Target {
  private final GwtFacet myFacet;
  private final GwtFacetConfiguration myConfiguration;

  public RunGwtCompilerTarget(GwtFacet facet, final GenerationOptions genOptions) {
    super(GwtBuildProperties.getRunGwtCompilerTargetName(facet), null, "Run GWT compiler", null);
    myFacet = facet;
    myConfiguration = myFacet.getConfiguration();
    addJavaTag(genOptions);
  }

  private void addJavaTag(final GenerationOptions genOptions) {
    Module module = myFacet.getModule();
    List<Pair> options = new ArrayList<>();
    options.add(pair("fork", "true"));
    String chunkName = genOptions.getChunkByModule(module).getName();
    if (genOptions.forceTargetJdk) {
      options.add(pair("jvm", BuildProperties.propertyRef(BuildProperties.getModuleChunkJdkBinProperty(chunkName)) + "/java"));
    }
    final GwtVersion sdkVersion = myFacet.getSdkVersion();
    options.add(pair("classname", sdkVersion.getCompilerClassName()));
    options.add(pair("failonerror", "true"));

    Tag java = new Tag("java", options.toArray(new Pair[0]));

    java.add(jvmarg("-Xmx" + myConfiguration.getCompilerMaxHeapSize() + "m"));
    String jvmParameters = myConfiguration.getAdditionalCompilerVMParameters();
    if (!StringUtil.isEmpty(jvmParameters)) {
      java.add(jvmarg(jvmParameters));
    }

    Tag classpath = new Tag("classpath");
    final String gwtDevJarName = myFacet.getSdkVersion().isUseSystemIndependentGwtDevJar() ? GwtSdkPathUtil.GWT_DEV_JAR : BuildProperties.propertyRef(GwtBuildProperties.getSystemDependentGwtSdkDevJarNameProperty());
    classpath.add(new PathElement(BuildProperties.propertyRef(GwtBuildProperties.getGwtSdkHomeProperty(myFacet))
                                  + "/" + gwtDevJarName));

    collectSourcePaths(genOptions, module, classpath);
    classpath.add(new PathRef(BuildProperties.getTestClasspathProperty(chunkName)));
    java.add(classpath);

    java.add(arg("-logLevel"));
    java.add(arg("WARN"));
    java.add(arg(sdkVersion.getCompilerOutputDirParameterName()));
    java.add(arg(BuildProperties.propertyRef(GwtBuildProperties.getGwtCompilerOutputPropertyName(myFacet))));
    java.add(arg("-style"));
    java.add(arg(myConfiguration.getOutputStyle().getId()));
    java.add(arg(BuildProperties.propertyRef(GwtBuildProperties.getGwtModuleParameter())));
    add(java);
  }

  private static void collectSourcePaths(final GenerationOptions genOptions, Module module, Tag classpath) {
    final Set<String> dependenciesWithGwtFacets = new LinkedHashSet<>();
    final OrderEnumerator enumerator = GwtClasspathUtil.enumerateEntriesWithGwtSourceRoots(module, true);
    enumerator.forEachModule(module1 -> {
      dependenciesWithGwtFacets.add(genOptions.getChunkByModule(module1).getName());
      return true;
    });
    for (String depChunkName : dependenciesWithGwtFacets) {
      classpath.add(new PathRef(BuildProperties.getSourcepathProperty(depChunkName)));
    }

    final Set<Library> libraries = new LinkedHashSet<>();
    enumerator.forEachLibrary(library -> {
      libraries.add(library);
      return true;
    });
    final File baseDir = BuildProperties.getProjectBaseDir(module.getProject());
    for (Library library : libraries) {
      LibraryDefinitionsGeneratorFactory.genLibraryContent(genOptions, library, OrderRootType.SOURCES, baseDir, classpath);
    }
  }

  private static Tag arg(final @NotNull @NonNls String arg) {
    return new Tag("arg", pair("value", arg));
  }

  private static Tag jvmarg(final @NotNull @NonNls String arg) {
    return new Tag("jvmarg", pair("line", arg));
  }
}
