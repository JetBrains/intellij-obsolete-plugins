package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.util.CvsVfsUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.HeavyPlatformTestCase;

import java.io.File;
import java.io.IOException;

/**
 * author: lesya
 */

public class RefreshTest extends HeavyPlatformTestCase {
  public static final int DIRS = 10;
  public static final String FILE_NAME = "file.txt";

  public void test() throws IOException {
    File root = FileUtil.createTempDirectory("refresh", "test");
    VirtualFile virtualRoot = CvsVfsUtil.refreshAndFindFileByIoFile(root);
    for (int i = 0; i < DIRS; i++){
      createChildDirectory(virtualRoot, createDirName(i));
    }

    for (int i = 0; i < DIRS; i++){
      final VirtualFile subDir = virtualRoot.findChild(createDirName(i));
       for (int j = 0; j < 100; j++){
         new File(new File(subDir == null ? "" : subDir.getPath()), FILE_NAME + j).createNewFile();
       }
      for (int j = 0; j < 100; j++){
       assertNotNull(CvsVfsUtil.refreshAndfFindChild(subDir, FILE_NAME + j));
      }
    }


  }

  private static String createDirName(int i) {
    return "dir" + i;
  }
}
