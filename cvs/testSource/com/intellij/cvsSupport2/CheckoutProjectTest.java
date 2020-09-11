package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.config.CvsConfiguration;
import com.intellij.cvsSupport2.cvsExecution.CvsOperationExecutor;
import com.intellij.cvsSupport2.cvsExecution.CvsOperationExecutorCallback;
import com.intellij.cvsSupport2.cvsExecution.ModalityContextImpl;
import com.intellij.cvsSupport2.cvshandlers.CommandCvsHandler;
import com.intellij.cvsSupport2.cvshandlers.CvsHandler;
import com.intellij.cvsSupport2.cvsoperations.cvsAdd.AddFileOperation;
import com.intellij.cvsSupport2.cvsoperations.cvsCheckOut.CheckoutFilesOperation;
import com.intellij.cvsSupport2.cvsoperations.cvsRemove.RemoveFilesOperation;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.netbeans.lib.cvsclient.command.KeywordSubstitution;

import java.io.File;


public class CheckoutProjectTest extends CvsTestsWorkingWithImportedProject {
  private static final String BINARY_CONTENT = "\n\n$Author: lesya $\n\n";
  private static final String BINARY_EXTENSION = "exe";
  private static final String INITIAL_SEPARATOR = "\n";

  public void testCheckoutProject() {
    File[] filesInProjectRoot = TestObject.getProjectDirectory().listFiles();
    assertEquals(1, filesInProjectRoot.length);
    assertEquals("CVS", filesInProjectRoot[0].getName());
  }

  public void testCheckoutWithRevision() throws Exception {
    processTestCheckoutWithRevisionForTestFile(true);
  }

  public void testCheckoutWithRevisionForBinaryFile() throws Exception {
    setIsBinary(TEST_FILE_EXTENSION);
    assertFalse(TEST_FILE.exists());
    processTestCheckoutWithRevisionForTestFile(true);
  }

  public void testCheckotRootDirectory() {
    checkoutProject();
    refreshFileSystem();
    try {
      CommandCvsHandler checkoutHandler = (CommandCvsHandler)CommandCvsHandler.createCheckoutFileHandler(
        createArrayOn(TestObject.getProjectDirectory().getVirtualFile()),
        CvsConfiguration.getInstance(myProject), PerformInBackgroundOption.DEAF);
      checkoutHandler.login(myProject);
      checkoutHandler.run(myProject, ModalityContextImpl.NON_MODAL);
    }
    catch (NullPointerException npe) {
      fail("Unexpected Exception");
    }
  }

  private void processTestCheckoutWithRevisionForTestFile(boolean replaceLineSeparators) throws Exception {
    String rev1Content = "rev 1" + LINE_SEPARATOR;
    TEST_FILE.changeContentTo(rev1Content);
    TEST_FILE.addToVcs(myVcs);
    commitTransaction();

    String rev2Content = "rev 2" + LINE_SEPARATOR;
    createNewTestFileRevisionWithContent(rev2Content);

    TEST_FILE.changeContentTo("changed content");

    checkoutTestFileForCurrentRevision();

    assertEquals(rev1Content, TEST_FILE.getContent());
    checkTestFileHasUpToDateStatus();

    removeTestFileLocally();
    refreshFileSystem();
    assertTrue(CvsUtil.getEntryFor(TEST_FILE).isRemoved());
    TEST_FILE.createInProject();
    checkoutTestFileForCurrentRevision();

    assertEquals(prepareExpected(replaceLineSeparators, rev1Content), TEST_FILE.getContent());
    checkTestFileHasUpToDateStatus();

    checkoutTestFileForLastRevision();

    assertEqualsAsText(prepareExpected(replaceLineSeparators, rev2Content), TEST_FILE.getContent());

    checkTestFileHasUpToDateStatus();
  }

  private static String prepareExpected(boolean replaceLineSeparators, String rev1Content) {
    return replaceLineSeparators ? StringUtil.convertLineSeparators(rev1Content, INITIAL_SEPARATOR) : rev1Content;
  }

  private void checkTestFileHasUpToDateStatus() {
    VirtualFile virtualFile = TEST_FILE.getVirtualFile();
    assertEquals(FileStatus.NOT_CHANGED, FileStatusManager.getInstance(myProject).getStatus(virtualFile));
  }

  private void removeTestFileLocally() throws Exception {
    RemoveFilesOperation operation = new RemoveFilesOperation();
    operation.addFile(TEST_FILE.getVirtualFile());
    assertTrue(TEST_FILE.deleteFromProject());
    assertFalse(TEST_FILE.exists());
    execute(operation);
  }

  private void checkoutTestFileForLastRevision() throws Exception {
    execute(new CheckoutFilesOperation(createArrayOn(TEST_FILE.getVirtualFile()),
                                       myConfiguration));
  }

  private void checkoutTestFileForCurrentRevision() {
    CvsOperationExecutor executor = new CvsOperationExecutor(myProject);
    VirtualFile parent = TEST_FILE.getVirtualFile().getParent();
    String name = TEST_FILE.getName();
    CvsHandler handler = CommandCvsHandler.createRestoreFileHandler(parent,
                                                                    name, false);
    executor.performActionSync(handler, CvsOperationExecutorCallback.EMPTY);
    refreshFileSystem();
  }

  public void testBynaryFiles() throws Exception {
    TestFile binaryFile = addBinaryFileToVcs();
    assertTrue(FileUtil.delete(TestObject.getProjectDirectory()));

    checkoutProject();

    assertEquals(BINARY_CONTENT, binaryFile.getContent());
  }

  private TestFile addBinaryFileToVcs() throws Exception {
    setIsBinary();
    TestFile binaryFile = new TestFile(TestObject.getProjectDirectory(), "binary." + BINARY_EXTENSION);
    binaryFile.changeContentTo(BINARY_CONTENT);

    AddFileOperation addOperation = new AddFileOperation(KeywordSubstitution.BINARY);
    addOperation.addFile(binaryFile.getAbsolutePath());
    execute(addOperation);

    myVcs.getStandardOperationsProvider().checkinFile(binaryFile.getAbsolutePath(), null, null);
    commitTransaction();
    return binaryFile;
  }

  private void setIsBinary() throws Exception {
    setIsBinary("exe");
  }

}
