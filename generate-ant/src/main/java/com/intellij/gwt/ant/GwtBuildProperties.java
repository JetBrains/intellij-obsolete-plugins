package com.intellij.gwt.ant;

import com.intellij.gwt.facet.GwtFacet;
import com.intellij.compiler.ant.BuildProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.NonNls;

/**
 * @author nik
 */
public class GwtBuildProperties {
  private GwtBuildProperties() {
  }

  @NotNull @NonNls
  public static String getGwtSdkHomeProperty() {
    return "gwt.sdk.home";
  }

  @NotNull @NonNls
  public static String getGwtSdkHomeProperty(@NotNull GwtFacet facet) {
    return "gwt.sdk.home." + getConvertedName(facet);
  }

  @NotNull @NonNls
  public static String getSystemDependentGwtSdkDevJarNameProperty() {
    return "gwt.sdk.dev.jar.name";
  }

  public static String getGwtCleanTargetName() {
    return "clean.gwt.compiler.output";
  }

  private static String getConvertedName(final GwtFacet facet) {
    return BuildProperties.convertName(facet.getModule().getName());
  }

  @NotNull @NonNls
  public static String getCompileGwtTargetName(@NotNull GwtFacet facet) {
    return "compile.gwt." + getConvertedName(facet);
  }

  @NotNull @NonNls
  public static String getRunGwtCompilerTargetName(@NotNull GwtFacet facet) {
    return "run.gwt.compiler." + getConvertedName(facet);
  }

  @NotNull @NonNls
  public static String getGwtCompilerOutputPropertyName(@NotNull GwtFacet facet) {
    return "gwt.compiler.output." + getConvertedName(facet);
  }

  @NotNull @NonNls
  public static String getGwtModuleParameter() {
    return "gwt.module.name";
  }
}
