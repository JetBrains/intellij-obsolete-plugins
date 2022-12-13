package com.intellij.dmserver.run;

import com.intellij.dmserver.install.DMServerInstallation;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import org.jetbrains.annotations.NonNls;

import java.io.File;

public final class DMServerCommandLineBase {
  private static final VirtualFileFilter VFS_LIST_JARS = pathname -> pathname != null && "jar".equals(pathname.getExtension());

  private static void defineTempDir(ParametersList vmParams, DMServerInstallation installation) {
    vmParams.defineProperty("java.io.tmpdir", relative2absolutePath(installation.getHome(), "work/temp"));
  }

  private static String relative2absolutePath(VirtualFile root, @NonNls String relativePath) {
    File file = new File(VfsUtilCore.virtualToIoFile(root), FileUtil.toSystemDependentName(relativePath));
    return file.getAbsolutePath();
  }

  private static String getAbsolutePath(VirtualFile file) {
    return FileUtil.toSystemDependentName(file.getPath());
  }
}
