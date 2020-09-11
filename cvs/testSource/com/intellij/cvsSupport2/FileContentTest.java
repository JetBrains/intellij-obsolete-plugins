package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.cvsoperations.cvsContent.GetFileContentOperation;
import com.intellij.cvsSupport2.cvsoperations.dateOrRevision.SimpleRevision;
import com.intellij.openapi.vcs.VcsException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * author: lesya
 */
public class FileContentTest extends CvsTestsWorkingWithImportedProject {

  private static final String REVISION2 = "1.2";
  private static final String REVISION3 = "1.3";

  private static final String CONTENT2 = "Content For Revision " + REVISION2;
  private static final String CONTENT3 = "Content For Revision " + REVISION3;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    addFile(TEST_FILE);
  }

  public void testGetContent() throws Exception {
    commitContent(CONTENT2);
    assertTrue(TEST_FILE.repositoryVersionContains(CONTENT2));
    assertEquals(CONTENT2, getContentForRevision(null));
    File projectDir = TEST_FILE.getParentFile();
    assertFalse(new File(projectDir, projectDir.getName()).exists());
  }

  public void testGetOneLineContent() throws Exception {
    String oneLineContent = "one line content";
    commitContent(oneLineContent);
    assertEquals(oneLineContent, getContentForRevision(null));
  }

  public void testContentForDifferentRevisions() throws Exception {
    commitContent(CONTENT2);
    commitContent(CONTENT3);

    assertEquals(CONTENT2, getContentForRevision(REVISION2));
    assertEquals(CONTENT3, getContentForRevision(REVISION3));

    assertEquals(REVISION3, getRevisionOfLastVersion());
  }

  public void testContentForLightweightFile() throws Exception {
    commitContent(CONTENT2);
    commitContent(CONTENT3);

    assertEquals(CONTENT2, getLightweightContentForRevision(REVISION2));
    assertEquals(CONTENT3, getLightweightContentForRevision(REVISION3));

    assertEquals(REVISION3, getRevisionOfLastVersion());
  }

  private String getLightweightContentForRevision(String revision2) throws Exception {
    GetFileContentOperation operation =
      new GetFileContentOperation(CvsUtil.getCvsLightweightFileForFile(TEST_FILE),
                                  this, new SimpleRevision(revision2));
    execute(operation);
    return new String(operation.getFileBytes(), StandardCharsets.UTF_8);
  }

  private Object getRevisionOfLastVersion() throws Exception {
    return executeGetContentOperation(null).getRevision();
  }

  private String getContentForRevision(String revision) throws Exception {
    byte[] bytes = executeGetContentOperation(revision).getFileBytes();
    assertNotNull(bytes);
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private GetFileContentOperation executeGetContentOperation(String revision) throws Exception {
    GetFileContentOperation operation = GetFileContentOperation.createForFile(TEST_FILE.getVirtualFile(),
                                                                              new SimpleRevision(revision));
    execute(operation);
    return operation;
  }

  private void commitContent(String content) throws IOException, VcsException {
    TEST_FILE.changeContentTo(content);
    myVcs.getStandardOperationsProvider().checkinFile(TEST_FILE.getCanonicalPath(), null, null);
    myVcs.commitTransaction(null);
  }
}
