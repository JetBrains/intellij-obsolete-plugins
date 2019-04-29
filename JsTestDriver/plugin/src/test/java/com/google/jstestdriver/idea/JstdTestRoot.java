package com.google.jstestdriver.idea;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFileManager;

import java.io.File;
import java.net.URL;

/**
 * @author Sergey Simonchik
 */
public class JstdTestRoot {
  private JstdTestRoot() {}

  public static File getTestDataDir() {
    return new File(PathManager.getResourceRoot(JstdTestRoot.class, "/root.txt"));
  }

}
