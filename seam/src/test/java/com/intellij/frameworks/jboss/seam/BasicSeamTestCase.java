package com.intellij.frameworks.jboss.seam;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.UsefulTestCase;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public abstract class BasicSeamTestCase extends UsefulTestCase {

  @Nullable
  protected static VirtualFile getFile(final String path) {

    final Ref<VirtualFile> result = new Ref<>(null);
    ApplicationManager.getApplication().runWriteAction(() -> {
      final VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
      result.set(file);
    });
    return result.get();
  }

  /**
   * Return relative path to the test data.
   *
   * @return relative path to the test data.
   */
  @NonNls
  protected String getBasePath() {
    return "/plugins/frameworks/jboss/seam/seam-tests/testData/";
  }

  /**
   * Return absolute path to the test data. Not intended to be overridden.
   *
   * @return absolute path to the test data.
   */
  @NonNls
  protected final String getTestDataPath() {
    return PathManager.getHomePath().replace(File.separatorChar, '/') + getBasePath();
  }
}
