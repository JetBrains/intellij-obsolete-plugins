package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.application.CvsEntriesManager;
import com.intellij.cvsSupport2.config.CvsConfiguration;
import com.intellij.cvsSupport2.cvsoperations.cvsAdd.AddFilesOperation;
import com.intellij.cvsSupport2.cvsoperations.cvsAdd.AddedFileInfo;
import com.intellij.cvsSupport2.cvsoperations.cvsContent.GetFileContentOperation;
import com.intellij.cvsSupport2.cvsoperations.cvsLog.RlogCommand;
import com.intellij.cvsSupport2.cvsoperations.cvsTagOrBranch.GetAllBranchesOperation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import org.netbeans.lib.cvsclient.admin.Entry;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

/**
 * author: lesya
 */
public class WorkingWithBranchesTest extends CvsTestsWorkingWithImportedProject {
  private static final String BRANCH_NAME = "mybranch";
  private static final String BRANCH_NAME_2 = "BR2";
  private static final String TAG = "TAG";

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    addFile(TEST_FILE);
    commitTransaction();
    createBranch(BRANCH_NAME);
  }


  public void testAddFileToBranch() throws Exception {
    setWorkingTag(BRANCH_NAME);
    checkoutProject();

    TestDirectory directoryInBranch = TestDirectory.createInProject("dirInBranche");
    directoryInBranch.mkdir();

    refreshFileSystem();

    directoryInBranch.addToVcs(myVcs);
    commitTransaction();

    TestFile fileInBranch = TestFile.createInProject("fileInBranch.txt");
    fileInBranch.createInProject();
    fileInBranch.addToVcs(myVcs);

    TestFile fileInDirInBranch = new TestFile(directoryInBranch, "file.txt");
    fileInDirInBranch.createInProject();
    fileInDirInBranch.addToVcs(myVcs);

    refreshFileSystem();

    commitTransaction();

    setWorkingTag(null);
    checkoutToAnotherLocation();
    assertFalse(getInAnotherLocation(fileInBranch).exists());
    assertFalse(getInAnotherLocation(fileInDirInBranch).exists());

    setWorkingTag(BRANCH_NAME);
    checkoutToAnotherLocation();
    assertTrue(getInAnotherLocation(TEST_FILE).exists());
    assertTrue(getInAnotherLocation(directoryInBranch).exists());
    assertTrue(getInAnotherLocation(fileInDirInBranch).exists());

  }

  public void testRemoveFileFromBranch() throws Exception {
    setWorkingTag(BRANCH_NAME);
    checkoutProject();
    TEST_FILE.delete();
    assertFalse(TEST_FILE.exists());
    TEST_FILE.deleteFromVcs(myVcs);
    commitTransaction();

    setWorkingTag(null);
    checkoutProject();
    assertTrue(TEST_FILE.exists());
  }

  public void testGetAllBranches() throws Exception {
    createBranch(BRANCH_NAME_2);
    createTag(TAG);
    RlogCommand command = new RlogCommand();
    executeCommand(command, new File(""));
    GetAllBranchesOperation op = new GetAllBranchesOperation(this);
    execute(op);
    assertEquals(ContainerUtil.set(BRANCH_NAME, BRANCH_NAME_2, TAG),
                 new HashSet<>(op.getAllBranches()));

  }

  public void testGetContentFromBranch() throws Exception {
    setWorkingTag(BRANCH_NAME);
    checkoutProject();
    String contentInBranch = "content in branch";
    TEST_FILE.changeContentTo(contentInBranch);
    myVcs.getStandardOperationsProvider().checkinFile(TEST_FILE.getAbsolutePath(), null, null);
    commitTransaction();

    checkContentIs(contentInBranch);

    setWorkingTag(null);
    checkoutProject();

    checkContentIs(TestFile.INITIAL_CONTENT);
  }

  public void testAddingALotOfDirectories() throws Exception {

    setWorkingTag(BRANCH_NAME);
    checkoutProject();


    final AddFilesOperation operation = new AddFilesOperation();

    final TestFile[] testFile = new TestFile[1];

    ApplicationManager.getApplication().runWriteAction(() -> {
      TestDirectory dir = TestObject.getProjectDirectory();

      for (int i = 0; i < 10; i++) {
        dir = new TestDirectory(dir, "dir");
        dir.mkdir();
        VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(dir);
        AddedFileInfo addedFileInfo = new AddedFileInfo(file, myProject, CvsConfiguration.getInstance(myProject));
        operation.addFile(file, addedFileInfo.getKeywordSubstitution());
      }

      testFile[0] = new TestFile(dir, "file.txt");
      try {
        testFile[0].createInProject();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
      VirtualFile file2 = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(testFile[0]);
      AddedFileInfo addedFileInfo = new AddedFileInfo(file2, myProject, CvsConfiguration.getInstance(myProject));
      operation.addFile(file2, addedFileInfo.getKeywordSubstitution());
    });

    refreshFileSystem();
    execute(operation);
    refreshFileSystem();
    testFile[0].addToVcs(myVcs);
    commitTransaction();

    setWorkingTag(null);
    checkoutProject();
    assertFalse(testFile[0].exists());

    setWorkingTag(BRANCH_NAME);
    checkoutProject();
    assertTrue(testFile[0].exists());

  }

  public void testCheckoutFileFromBranch() throws Exception {
    setWorkingTag(BRANCH_NAME);
    checkoutProject();
    TestFile fileInBranch = TestFile.createInProject("fileInBranch.txt");
    fileInBranch.createInProject();
    fileInBranch.addToVcs(myVcs);
    commitTransaction();

    checkoutFile(fileInBranch);

    refreshFileSystem();

    VirtualFile virtualFile = fileInBranch.getVirtualFile();
    Entry entry = CvsEntriesManager.getInstance().getEntryFor(virtualFile.getParent(), virtualFile.getName());
    assertEquals(BRANCH_NAME, entry.getStickyTag());

  }

//  public void testGetAllFilesCommand() throws Exception {
//    myConfig.HOST = "cvsdev.labs.intellij.net";
//    myConfig.REPOSITORY="/aurora";
//    myConfig.PASSWORD = "ARSR_2?E";
//    final GetAllFilesCommand command = new GetAllFilesCommand();
//    long start = System.currentTimeMillis();
//    execute(new LocalPathIndifferentOperation(this){
//      protected Command createCommand(CvsRootProvider root) {
//        return command;
//      }
//
//      public void modifyOptions(GlobalOptions options) {
//        super.modifyOptions(options);
//        options.setDoNoChanges(true);
//      }
//    }, CvsMessagesListener.DEAF);
//  }

  private void checkContentIs(String contentInBranch) throws Exception {
    GetFileContentOperation op = GetFileContentOperation.createForFile(TEST_FILE.getVirtualFile());
    execute(op);
    assertEquals(contentInBranch, new String(op.getFileBytes(), StandardCharsets.UTF_8));
  }

}
