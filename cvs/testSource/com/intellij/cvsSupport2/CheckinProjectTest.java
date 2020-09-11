package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.cvsoperations.cvsRemove.RemoveFilesOperation;
import com.intellij.cvsSupport2.cvsstatuses.CvsStatusProvider;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.io.IOException;

public class CheckinProjectTest extends CvsTestsWorkingWithImportedProject {

  public void testAddingFile() throws Exception {
    addFile(TEST_FILE);
  }

  public void testCommitBehavior() throws Exception {
    addFile(TEST_FILE);
    myCreatedConnections.clear();

    myVcs.getStandardOperationsProvider().addFile(TEST_FILE.getParent(), TEST_FILE.getName(), null, null);
    try {
      commitTransaction();
    }
    catch (VcsException e) {
      return;
    }
    fail("Exception expected");
  }

  public void testAddingDirectory() throws Exception {
    addFile(TEST_DIRECTORY);
  }

  public void testDeletingFile() throws Exception {
    doTestDeleting(TEST_FILE);
  }

  private void doTestDeleting(TestObject testFile) throws Exception {
    addFileToRepository(testFile);
    assertTrue(testFile.deleteFromProject());
    testFile.deleteFromVcs(myVcs);
    assertTrue(testFile.isInRepository());
    commitTransaction();
    assertFalse(testFile.isInRepository());
  }

  public void testCheckinFile() throws Exception {
    addFileToRepository(TEST_FILE);
    TEST_FILE.changeContentTo(NEW_CONTENT);
    myVcs.getStandardOperationsProvider().checkinFile(TEST_FILE.getAbsolutePath(), null, null);
    assertTrue(!TEST_FILE.repositoryVersionContains(NEW_CONTENT));
    commitTransaction();
    assertTrue(TEST_FILE.repositoryVersionContains(NEW_CONTENT));
  }

  public void testCheckinRequestedFileOnly() throws Exception {
    addFileToRepository(FILE_TO_EXCLUDE);
    addFileToRepository(TEST_FILE);

    TEST_FILE.changeContentTo(NEW_CONTENT);
    FILE_TO_EXCLUDE.changeContentTo(NEW_CONTENT);

    myVcs.getStandardOperationsProvider().checkinFile(TEST_FILE.getAbsolutePath(), null, null);
    commitTransaction();

    assertFalse(FILE_TO_EXCLUDE.repositoryVersionContains(NEW_CONTENT));
  }

  public void testCheckinProject() throws Exception {
    doSomeChanges();

    int expectedFileMessagesCount = 8;
    /*
      sending added
      scheduled for adding
      sheduled for removal
      sending added
      added
      removed
      sending changed
      saved
    */

    assertEquals(expectedFileMessagesCount, myVcs.getFilesToProcessCount());

    commitTransaction();

    checkChangesWasCommited();

    assertTrue(FILE_TO_ADD.repositoryVersionContains(COMMENT));
    assertTrue(FILE_TO_CHECKIN.repositoryVersionContains(COMMENT));
  }

  public void testRollbackTransaction() throws Exception {
    doSomeChanges();

    myVcs.rollbackTransaction(null);

    checkNoChangesInRepository();

    commitTransaction();

    checkNoChangesInRepository();
  }

  public void testDeletingNotEmptyDirectory() throws Exception {
    addFileToRepository(TEST_DIRECTORY);
    TestFile fileInDirectory = new TestFile(TEST_DIRECTORY, "file.txt");
    addFileToRepository(fileInDirectory);

    fileInDirectory.deleteFromProject();
    TEST_DIRECTORY.deleteFromProject();

    fileInDirectory.deleteFromVcs(myVcs);
    TEST_DIRECTORY.deleteFromVcs(myVcs);

    refreshFileSystem();

    commitTransaction();

    File[] files = TEST_DIRECTORY.myRepositoryVersion().listFiles();

    if (files.length == 3) {
      assertEquals(".owner", files[0].getName());
      assertEquals(".perms", files[1].getName());
      assertEquals("Attic", files[2].getName());
    } else if (files.length == 2) {
      assertEquals("CVS", files[0].getName());
      assertEquals(fileInDirectory.getName() + ",v", files[1].getName());
    }
  }

  public void testWorkingInSubdirectory() throws IOException, VcsException {
    addFile(TEST_DIRECTORY);

    TestObject file = new TestFile(TEST_DIRECTORY.getAbsoluteFile(), "fileInDirectory.txt");
    file.createInProject();
    file.addToVcs(myVcs);
    commitTransaction();

    file.deleteFromProject();
    file.deleteFromVcs(myVcs);
    commitTransaction();

    assertFalse(file.isInRepository());
  }

  public void testEOFException() throws Exception {
    addFileToRepository(TEST_FILE);

    TEST_FILE.changeContentTo("1");
    TEST_FILE.setLastModified(System.currentTimeMillis() + 1000);
    myVcs.getStandardOperationsProvider().checkinFile(TEST_FILE.getAbsolutePath(), null, null);
    commitTransaction();

    TEST_FILE.changeContentTo("2");
    TEST_FILE.setLastModified(System.currentTimeMillis() + 2000);
    myVcs.getStandardOperationsProvider().checkinFile(TEST_FILE.getAbsolutePath(), null, null);
    commitTransaction();
  }

  public void testAddingLocallyRemovedFile() throws Exception {
    TestDirectory folder1 = new TestDirectory(TestObject.getProjectDirectory(), "folder1");
    addFileToRepository(folder1);

    TestDirectory folder2 = new TestDirectory(folder1, "folder2");
    addFileToRepository(folder2);

    TestDirectory folder3 = new TestDirectory(folder2, "folder3");
    addFileToRepository(folder3);

    TestFile file = new TestFile(folder3, "file.txt");
    file.createInProject();

    file.addToVcs(myVcs);
    commitTransaction();

    file.deleteFromFileSystem();

    refreshFileSystem();

    RemoveFilesOperation removeFilesOperation = new RemoveFilesOperation();
    removeFilesOperation.addFile(file.getAbsolutePath());
    execute(removeFilesOperation);

    String content = "new content";
    file.createInProject();
    file.changeContentTo(content);
    assertEquals(content, file.getContent());

    long timeStamp = file.lastModified();

    VirtualFile vFile = file.getVirtualFile();
    CvsUtil.restoreFile(vFile);

    refreshFileSystem();

    assertEquals(content, file.getContent());
    assertTrue(CvsUtil.fileIsUnderCvs(vFile));
    assertEquals(FileStatus.MODIFIED, CvsStatusProvider.getStatus(vFile));
    assertFalse(CvsUtil.fileIsLocallyRemoved(vFile));
    assertEquals(timeStamp, file.lastModified());
  }

  private static void checkChangesWasCommited() throws IOException {
    assertTrue(FILE_TO_ADD.isInRepository());
    assertFalse(FILE_TO_REMOVE.isInRepository());
    assertTrue(FILE_TO_CHECKIN.repositoryVersionContains(NEW_CONTENT));
    assertFalse(FILE_TO_EXCLUDE.repositoryVersionContains(NEW_CONTENT));
  }

  private static void checkNoChangesInRepository() throws IOException {
    assertFalse(FILE_TO_ADD.isInRepository());
    assertTrue(FILE_TO_REMOVE.isInRepository());
    assertFalse(FILE_TO_CHECKIN.repositoryVersionContains(NEW_CONTENT));
    assertFalse(FILE_TO_EXCLUDE.repositoryVersionContains(NEW_CONTENT));
  }

  private void doSomeChanges() throws Exception {
    createInitialStructure();

    doLocalChanges();

    putChangesToVcs();
  }

  private void putChangesToVcs() throws VcsException {
    myVcs.getStandardOperationsProvider().addFile(PROJECT_PATH, FILE_TO_ADD.getName(), null, null);
    myVcs.getStandardOperationsProvider().removeFile(FILE_TO_REMOVE.getAbsolutePath(), null, null);
    myVcs.getStandardOperationsProvider().checkinFile(FILE_TO_CHECKIN.getAbsolutePath(), null, null);
  }

  private static void doLocalChanges() throws IOException {
    FILE_TO_ADD.createInProject();
    FILE_TO_CHECKIN.changeContentTo(NEW_CONTENT);
    FILE_TO_EXCLUDE.changeContentTo(NEW_CONTENT);
    FILE_TO_REMOVE.delete();
  }

  private void createInitialStructure() throws Exception {
    addFileToRepository(FILE_TO_REMOVE);
    addFileToRepository(FILE_TO_CHECKIN);
    addFileToRepository(FILE_TO_EXCLUDE);
  }

  private void addFileToRepository(TestObject file) throws Exception {
    file.createInProject();
    file.addToVcs(myVcs);
    commitTransaction();
  }

}
