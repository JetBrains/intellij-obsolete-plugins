package org.jetbrains.jps.gwt.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.index.JpsGwtModule;
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.module.JpsModule;

public interface JpsGwtModuleExtension extends JpsElement {
  @NotNull
  JpsModule getModule();

  String getCompilerParameters();

  String getAdditionalCompilerVMParameters();

  int getCompilerMaximumHeapSize();

  GwtJavaScriptOutputStyle getOutputStyle();

  String getPackagingRelativePath(JpsGwtModule module);

  boolean isModuleCompilationEnabled(JpsGwtModule module);

  @NotNull
  GwtSdkPaths getSdkPaths();
}
