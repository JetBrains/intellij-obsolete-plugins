package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.application.CvsEntriesManager;
import com.intellij.cvsSupport2.config.CvsApplicationLevelConfiguration;
import com.intellij.cvsSupport2.cvsoperations.cvsCheckOut.CheckoutProjectOperation;
import com.intellij.cvsSupport2.javacvsImpl.FileReadOnlyHandler;
import com.intellij.cvsSupport2.util.CvsVfsUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.util.io.ByteBufferWrapper;
import org.netbeans.lib.cvsclient.admin.EntriesHandler;
import org.netbeans.lib.cvsclient.admin.Entry;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;

/**
 * author: lesya
 */
public class UpdateTest extends CvsTestsWorkingWithImportedProject {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    createTestFileAndDirectoryInRepository();
  }

  public void testUpdateDirectory() throws Exception {
    TestFile testFile = new TestFile(TEST_DIRECTORY, "file.txt");
    testFile.createInProject();
    testFile.addToVcs(myVcs);
    commitTransaction();

    assertTrue(TEST_FILE.deleteFromProject());
    deleteTestDirectoryFromProject();

    updateProjectDirectory();

    assertTrue(TEST_FILE.exists());
    assertTrue(TEST_DIRECTORY.exists());
  }

  public void testUpdatesRequestedFileOnly() throws Exception {
    changeTestFileContentInRepository();
    deleteTestDirectoryFromProject();

    updateTestFile();

    assertTrue(TEST_FILE.projectVersionContains(NEW_CONTENT));
    assertFalse(TEST_DIRECTORY.exists());
  }

  public void testUpdateReadOnlyFiles() throws Exception {
    setReadOnlyAttributeToTestFile();

    changeTestFileContentInRepository();

    updateTestFile();

    assertTrue(TEST_FILE.projectVersionContains(NEW_CONTENT));
  }

  public void testUpdatingLockedFile() throws Exception {

    long timeStamp = TEST_FILE.lastModified();

    deleteFileInVcs(TEST_FILE);

    String revision = CvsUtil.getEntryFor(TEST_FILE).getRevision();

    ByteBufferWrapper holder = ByteBufferWrapper.readWriteDirect(TEST_FILE.toPath(), 0, (int) Files.size(TEST_FILE.toPath()));
    ByteBuffer holdToPreventGC = holder.getBuffer();

    TEST_FILE.setLastModified(timeStamp);

    try {
      try {
        updateProjectDirectory();
      }
      catch (VcsException ex) {
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().startsWith("Could not delete file"));
      }
      assertEquals(revision, CvsUtil.getEntryFor(TEST_FILE).getRevision());
    }
    finally {
      holdToPreventGC = null;
      holder.release();
    }

  }

  public void testDeletingReadOnlyFile() throws Exception {
    deleteTestFileFromCvs();
    setTestFileReadOnly();
    assertTrue(TEST_FILE.exists());
    updateProjectDirectory();
    assertFalse(TEST_FILE.exists());
  }


  public void testUpdateFilesWithLocked() throws Exception {
    TestFile[] files = new TestFile[10];
    for (int i = 0; i < files.length; i++) {
      TestFile file = TestFile.createInProject("newFile" + i + ".txt");
      file.createInProject();
      addFile(file);
      changeContentInRepository(file);
    }

    TestFile lockedFile = TestFile.createInProject("newFile5.txt");

    String lockedRevision = CvsEntriesManager.getInstance().getEntryFor(CvsVfsUtil.getParentFor(lockedFile),
                                                                        lockedFile.getName())
      .getRevision();

    ByteBufferWrapper holder = ByteBufferWrapper.readWriteDirect(lockedFile.toPath(), 0, (int) Files.size(lockedFile.toPath()));
    ByteBuffer holdToPreventGC = holder.getBuffer();

    try {
      updateProjectDirectory();
      if (myErrors.size() != 0){
        fail(myErrors.iterator().next().toString());
      }

      assertEquals(1, myWarnings.size());
      assertTrue(((Exception)myWarnings.iterator().next()).getMessage().startsWith("Could not delete file"));
      for (int i = 0; i < 10; i++) {
        TestFile file = TestFile.createInProject("newFile" + i + ".txt");
        if (!file.getName().equals(lockedFile.getName())) {
          assertEquals(NEW_CONTENT, file.getContent());
        }
        else {
          Entry entryAfterUpdate = CvsEntriesManager.getInstance().getEntryFor(CvsVfsUtil.getParentFor(lockedFile),
                                                                               lockedFile.getName());
          assertEquals(lockedRevision, entryAfterUpdate.getRevision());
        }
      }
    }
    finally {
      holdToPreventGC = null;
      holder.release();
    }

  }

  public void testCreatingEntryInParentDir() throws Exception {
    refreshFileSystem();
    String name = TEST_DIRECTORY.getName();

    removeEntryNamed(name);

    updateProjectDirectory();

    checkEntryExists(name);
  }

  private static void checkEntryExists(String name) throws IOException {
    EntriesHandler entriesAfterUpdate = new EntriesHandler(TestObject.getProjectDirectory());
    entriesAfterUpdate.read(CvsApplicationLevelConfiguration.getCharset());
    assertNotNull(entriesAfterUpdate.getEntries().getEntry(name));
  }

  private static void removeEntryNamed(String name) throws IOException {
    EntriesHandler entries = new EntriesHandler(TestObject.getProjectDirectory());
    entries.read(CvsApplicationLevelConfiguration.getCharset());
    entries.getEntries().removeEntry(name);
    entries.write(CodeStyleSettingsManager.getInstance().getCurrentSettings().getLineSeparator(),
      CvsApplicationLevelConfiguration.getCharset());

    refreshFileSystem();
  }

  private static void setTestFileReadOnly() throws IOException {
    new FileReadOnlyHandler().setFileReadOnly(TEST_FILE, true);
  }

  private void deleteTestFileFromCvs() throws Exception {
    checkoutToAnotherLocation();
    removeOnServer(TEST_FILE);
  }

  private void deleteFileInVcs(TestFile file) throws Exception {
    checkoutToAnotherLocation();
    TestFile another = getInAnotherLocation(file);
    another.delete();
    myVcs.getStandardOperationsProvider().removeFile(another.getAbsolutePath(), null, null);
    commitTransaction();
  }

  private static void setReadOnlyAttributeToTestFile() throws IOException {
    new FileReadOnlyHandler().setFileReadOnly(TEST_FILE, true);
  }

  private static void deleteTestDirectoryFromProject() {
    assertTrue(FileUtil.delete(TEST_DIRECTORY));
  }


  private void changeTestFileContentInRepository() throws Exception {
    changeContentInRepository(TEST_FILE);
  }

  private void changeContentInRepository(TestFile file) throws Exception {
    File anotherWorkingDirectory = new File(TestObject.getWorkingFolder(), "another");
    FileUtil.delete(anotherWorkingDirectory);
    anotherWorkingDirectory.mkdir();
    execute(CheckoutProjectOperation.createTestInstance(this, file.nameInRepository(), anotherWorkingDirectory));
    File newProjectLocation = new File(anotherWorkingDirectory, TestObject.getProjectDirectory().getName());
    TestFile newTestFileLication = new TestFile(newProjectLocation, file.getName());
    newTestFileLication.changeContentTo(NEW_CONTENT);
    myVcs.getStandardOperationsProvider().checkinFile(newTestFileLication.getAbsolutePath(), null, null);
    commitTransaction();
  }

  private void createTestFileAndDirectoryInRepository() throws IOException, VcsException {
    TEST_FILE.createInProject();
    TEST_DIRECTORY.createInProject();
    TEST_FILE.addToVcs(myVcs);
    TEST_DIRECTORY.addToVcs(myVcs);
    commitTransaction();
  }

}
