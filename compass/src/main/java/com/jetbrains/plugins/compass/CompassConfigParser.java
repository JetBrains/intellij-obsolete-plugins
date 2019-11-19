package com.jetbrains.plugins.compass;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class CompassConfigParser {
  protected static final String ADD_IMPORT_PATH_CALL = "add_import_path";
  protected static final String ADDITIONAL_IMPORT_PATHS_ASSIGNMENT = "additional_import_paths";

  
  @NotNull
  public final CompassConfig parse(@NotNull VirtualFile file, @Nullable PsiManager psiManager) {
    final VirtualFile parentFile = file.getParent();
    final String parentPath = parentFile != null ? parentFile.getPath() : "";
    return parse(file, parentPath, psiManager);
  }

  @NotNull
  public abstract CompassConfig parse(@NotNull VirtualFile file, @NotNull String importPathsRoot, @Nullable PsiManager psiManager);


  protected static String normalizePath(@NotNull final String path, @NotNull final String configParentPath) {
    return FileUtil.isAbsolutePlatformIndependent(path) ? path : FileUtil.toCanonicalPath(FileUtil.join(configParentPath, path));
  }
}
