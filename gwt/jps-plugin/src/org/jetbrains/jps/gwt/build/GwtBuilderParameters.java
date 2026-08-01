package org.jetbrains.jps.gwt.build;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.incremental.CompileContext;

public final class GwtBuilderParameters {
  public static final @NonNls String GWT_COMPILE_REPORT_PARAMETER = "GWT_COMPILE_REPORT_SOURCE";

  public static @NotNull String generateCompileReportParameter(@NotNull String moduleName, @NotNull String gwtModuleQualifiedName) {
    return moduleName + ":" + gwtModuleQualifiedName;
  }

  public static @Nullable String getCompileReportModuleName(CompileContext context) {
    String compileReportParameter = context.getBuilderParameter(GWT_COMPILE_REPORT_PARAMETER);
    if (compileReportParameter == null) return null;
    return compileReportParameter.substring(0, compileReportParameter.indexOf(':'));
  }

  public static @Nullable String getCompileReportGwtModuleQualifiedName(CompileContext context) {
    String compileReportParameter = context.getBuilderParameter(GWT_COMPILE_REPORT_PARAMETER);
    if (compileReportParameter == null) return null;
    return compileReportParameter.substring(compileReportParameter.indexOf(':')+1);
  }
}
