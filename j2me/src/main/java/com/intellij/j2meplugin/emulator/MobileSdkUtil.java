/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.emulator;

import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;

public class MobileSdkUtil {
  @NonNls private static final String JAR_FILE_TYPE = ".jar";
  @NonNls private static final String ZIP_FILE_TYPE = ".zip";
  @NonNls private static final String EXT_DIR = "ext";

  @NotNull
  public static VirtualFile[] findApiClasses(@NotNull String[] api) {
    JarFileSystem jarFileSystem = JarFileSystem.getInstance();

    ArrayList<VirtualFile> result = new ArrayList<>();
    for (int i = 0; i < api.length; i++) {
      File child = new File(api[i]);
      String path = child.getAbsolutePath().replace(File.separatorChar, '/') + JarFileSystem.JAR_SEPARATOR;
      jarFileSystem.setNoCopyJarForPath(path);
      VirtualFile vFile = jarFileSystem.findFileByPath(path);
      if (vFile != null) {
        result.add(vFile);
      }
    }
    return VfsUtilCore.toVirtualFileArray(result);
  }

  @NotNull
  public static VirtualFile[] findApiClasses(@NotNull File file) {
    FileFilter jarFileFilter = f -> {
      if (f.isDirectory()) return false;
      if (f.getName().endsWith(JAR_FILE_TYPE) ||
          f.getName().endsWith(ZIP_FILE_TYPE)) {
        return true;
      }
      return false;
    };

    @NonNls final String libDirName = "lib";
    final File lib = new File(file, libDirName);
    File[] jarDirs = {lib, new File(lib, EXT_DIR)};

    ArrayList<File> childrenList = new ArrayList<>();
    for (File jarDir : jarDirs) {
      if (jarDir.isDirectory()) {
        File[] files = jarDir.listFiles(jarFileFilter);
        if (files != null) {
          Collections.addAll(childrenList, files);
        }
      }
    }

    JarFileSystem jarFileSystem = JarFileSystem.getInstance();

    ArrayList<VirtualFile> result = new ArrayList<>();
    for (File child : childrenList) {
      String path = child.getAbsolutePath().replace(File.separatorChar, '/') + JarFileSystem.JAR_SEPARATOR;
      jarFileSystem.setNoCopyJarForPath(path);
      VirtualFile vFile = jarFileSystem.findFileByPath(path);
      if (vFile != null) {
        result.add(vFile);
      }
    }
    return VfsUtilCore.toVirtualFileArray(result);
  }

  public static void findDocs(File root, ArrayList<? super VirtualFile> docs) {
    if (!root.exists() || !root.isDirectory()) return;
    File[] docFiles = root.listFiles(MobileSdkUtil::isDocRoot);
    if (docFiles != null && docFiles.length > 0) {
      String path = root.getAbsolutePath().replace(File.separatorChar, '/');
      docs.add(LocalFileSystem.getInstance().findFileByPath(path));
    }

    File[] children = root.listFiles(pathname -> !isDocRoot(pathname));
    for (int i = 0; children != null && i < children.length; i++) {
      findDocs(children[i], docs);
    }
  }

  private static boolean isDocRoot(File pathname) {
    @NonNls final String path = pathname.getPath();
    if (path.endsWith("index.htm") || path.endsWith("index.html")) {
      return true;
    }
    return false;
  }

}
