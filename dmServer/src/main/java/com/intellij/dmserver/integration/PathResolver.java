package com.intellij.dmserver.integration;

import com.intellij.dmserver.util.PathUtils;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;

public abstract class PathResolver {

  public String path2Absolute(String path) {
    return FileUtil.isAbsolute(FileUtil.toSystemDependentName(path))
           ? path
           : PathUtils.concatPaths(getBaseDir().getPath(), path);
  }

  public String path2Relative(String path) {
    String independentPath = FileUtil.toSystemIndependentName(path);
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(independentPath);
    if (file != null && VfsUtilCore.isAncestor(getBaseDir(), file, true)) {
      String relativePath = VfsUtilCore.getRelativePath(file, getBaseDir(), '/');
      if (relativePath != null) {
        return relativePath;
      }
    }
    return independentPath;
  }

  protected abstract VirtualFile getBaseDir();
}
