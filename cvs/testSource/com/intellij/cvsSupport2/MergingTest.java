package com.intellij.cvsSupport2;

import com.intellij.openapi.vcs.VcsException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * author: lesya
 */

public class MergingTest extends CvsTestsWorkingWithImportedProject{
  private static final String CONTENT_FROM_ANOTHER_LOCATION = "content from another location\n";
  private static final String CONTENT_IN_THIS_LOCATION = "Content in this location\n";

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    setCopyMergingMode();
  }

  public void testUpdate() throws Exception {
    addFile(TEST_FILE);

    checkinTestFileFromAnotherLocationWithContent(CONTENT_FROM_ANOTHER_LOCATION);

    changeLocalContentTo(CONTENT_IN_THIS_LOCATION);

    TEST_FILE.setLastModified(TEST_FILE.lastModified() + 4000);

    refreshFileSystem();

    updateTestFile();

    assertEquals(CONTENT_IN_THIS_LOCATION, new String(CvsUtil.getStoredContentForFile(TEST_FILE.getVirtualFile()), StandardCharsets.UTF_8));
  }

  public void testCheckin() throws Exception {
    addFile(TEST_FILE);

    checkinTestFileFromAnotherLocationWithContent(CONTENT_FROM_ANOTHER_LOCATION);

    changeLocalContentTo(CONTENT_IN_THIS_LOCATION);

    try {
      attemptToCheckinTestFile();
    }
    catch (VcsException e) {
      assertEquals(CONTENT_IN_THIS_LOCATION, TEST_FILE.getContent());
      return;
    }

    fail("Exception expected");

  }

  private void attemptToCheckinTestFile() throws VcsException, IOException {
    myVcs.getStandardOperationsProvider().checkinFile(TEST_FILE.getCanonicalPath(), null, null);
    commitTransaction();
  }

  private static void changeLocalContentTo(String content) throws IOException {
    TEST_FILE.changeContentTo(content);
  }

  private void setCopyMergingMode() throws Exception {
    appendStringsToFileInCvsRoot("cvswrappers", new String[]{"-m COPY"});
  }

  private void checkinTestFileFromAnotherLocationWithContent(String contentFromAnotherLocation) throws Exception {
    checkoutProjectTo(myAnotherLocation);

    TestFile anotherTestFile = getTestFileIn(myAnotherLocation);
    anotherTestFile.changeContentTo(contentFromAnotherLocation);
    myVcs.getStandardOperationsProvider().checkinFile(anotherTestFile.getCanonicalPath(), null, null);
    commitTransaction();

  }
}
