package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.cvsstatuses.CvsStatusProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vcs.FileStatus;

import java.io.IOException;

/**
 * author: lesya
 */
public class CvsStatusTest extends CvsTestsWorkingWithImportedProject{

  public void testMergedWithConflict() throws Exception {
    String content = "2\n3\n4\n5\n";

    TEST_FILE.changeContentTo(content);
    TEST_FILE.addToVcs(myVcs);
    commitTransaction();

    createNewTestFileRevisionWithContent("2\n3\n4\n5\n6\n");

    TEST_FILE.changeContentTo("content3");
    TEST_FILE.setLastModified(TEST_FILE.lastModified() + 5000);

    refreshFileSystem();

    updateTestFile();

    refreshTestFile();


    checkTestFileStatusIs(FileStatus.MERGED_WITH_CONFLICTS);

    createNewTestFileRevisionWithContent("2\n3\n4\n5\n6\n");
    TEST_FILE.changeContentTo("1\n2\n3\n4\n5\n");
    updateTestFile();
    checkTestFileStatusIs(FileStatus.MERGE);
  }

  private static void checkTestFileStatusIs(FileStatus status) {
    assertEquals(status, CvsStatusProvider.getStatus(TEST_FILE.getVirtualFile()));
  }

  public void testMerged() throws Exception {
    String content = "2\n3\n4\n5\n";
    TEST_FILE.changeContentTo(content);
    TEST_FILE.addToVcs(myVcs);
    commitTransaction();

    createNewTestFileRevisionWithContent("2\n3\n4\n5\n6\n");

    TEST_FILE.changeContentTo("1\n2\n3\n4\n5\n");
    TEST_FILE.setLastModified(TEST_FILE.lastModified() + 5000);

    refreshFileSystem();

    updateTestFile();

    refreshTestFile();

    checkTestFileStatusIs(FileStatus.MERGE);
  }

  public void testIgnored() throws IOException {
    TEST_FILE.createInProject();
    //TEST_FILE.addToVcs(myVcs);
    //commitTransaction();

    CvsUtil.ignoreFile(TEST_FILE.getVirtualFile());

    refreshTestFile();

    checkTestFileStatusIs(FileStatus.IGNORED);

  }

  private static void refreshTestFile() {
    ApplicationManager.getApplication().runWriteAction(() -> TEST_FILE.getVirtualFile().refresh(false, true));
  }

}
