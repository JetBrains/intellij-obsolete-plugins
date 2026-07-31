package org.jetbrains.jps.gwt.index;

import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.module.JpsModule;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public interface JpsGwtModule {
  @NotNull Path getModuleFile();

  @NotNull
  List<File> getPublicRoots(boolean includeTests);

  @NotNull
  List<File> getSourceRoots(boolean includeTests);

  boolean isInTests();

  boolean hasEntryPoints();

  String getOutputName();

  @NlsSafe String getQualifiedName();

  @NotNull
  JpsModule getModule();

  List<String> getInheritedNames();
}
