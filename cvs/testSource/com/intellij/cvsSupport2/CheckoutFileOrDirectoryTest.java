package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.cvsoperations.cvsContent.GetFileContentOperation;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFileManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author lesya
 */
public class CheckoutFileOrDirectoryTest extends CvsTestsWorkingWithImportedProject {
  private final TestDirectory myDirectory = TestDirectory.createInProject("dir");
  private final TestFile myFile1 = new TestFile(myDirectory, "file1.txt");
  private final TestFile myFile2 = new TestFile(myDirectory, "file2.txt");
  private final TestFile myFileNotInRepository = new TestFile(myDirectory, "file3.txt");

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    myDirectory.createInProject();
    myDirectory.addToVcs(myVcs);
    commitTransaction();

    myFile1.createInProject();
    myFile2.createInProject();

    refreshFileSystem();

    myFile1.addToVcs(myVcs);
    myFile2.addToVcs(myVcs);
    commitTransaction();
    checkoutProject();
  }


  public void testLineSeparators() throws Exception {
    String content = "111\r\n222\r\n333";
    String repositoryContent = "111\n222\n333";
    myFile1.changeContentTo(content);
    checkinFirstFile();
    assertEquals(repositoryContent, getRepositoryContentOf(myFile1));
    checkinFirstFile();
    assertEquals(repositoryContent, getRepositoryContentOf(myFile1));
    checkinFirstFile();
    assertEquals(repositoryContent, getRepositoryContentOf(myFile1));
  }


  public void testCheckoutDirectory() throws IOException {
    changeFilesLocally();
    checkoutFile(myDirectory);
    assertEqualsAsText(TestFile.INITIAL_CONTENT, myFile1.getContent());
    assertEqualsAsText(TestFile.INITIAL_CONTENT, myFile2.getContent());
    assertTrue(myFileNotInRepository.exists());
  }


  public void testCheckoutFile() throws IOException {
    changeFilesLocally();
    checkoutFirstFile();

    assertEqualsAsText(TestFile.INITIAL_CONTENT, myFile1.getContent());

    assertEqualsAsText(NEW_CONTENT, myFile2.getContent());

    assertTrue(myFileNotInRepository.exists());

  }

  public void testCheckoutFileFromModule() throws Exception {

    TestDirectory dir1 = TestDirectory.createInProject("dir1");
    TestDirectory dir2 = new TestDirectory(dir1, "dir2");
    TestDirectory dir3 = new TestDirectory(dir2, "dir3");
    TestDirectory dir4 = new TestDirectory(dir3, "dir4");

    TestFile testFile = new TestFile(dir4, "file.txt");

    VirtualFileManager.getInstance().syncRefresh();

    addFile(dir1);
    addFile(dir2);
    addFile(dir3);
    addFile(dir4);

    addFile(testFile);

    String moduleName = "module";
    addModule(moduleName, getModuleName() + "/dir1/dir2/dir3/dir4");

    TestObject.getProjectDirectory().deleteFromFileSystem();

    checkoutModule(moduleName);

    refreshFileSystem();

    TestDirectory moduleDirectory = new TestDirectory(getProjectRoot(), moduleName);
    TestFile fileInModule = new TestFile(moduleDirectory, testFile.getName());

    fileInModule.changeContentTo("modified");

    checkoutProject();


    refreshFileSystem();

    checkoutFile(fileInModule);

    assertEqualsAsText(TestFile.INITIAL_CONTENT,  fileInModule.getContent());

  }

  public void testGetFileContent() throws Exception{
    checkGetContent("111\n222\n333\n");
    checkGetContent("111\n222\n333");

  }

  private void checkGetContent(final String content) throws Exception {
    myFile1.changeContentTo(content);
    checkinFirstFile();
    assertEquals(content, getRepositoryContentOf(myFile1));
  }

  private void checkoutModule(String moduleName) {
    checkoutModuleTo(getProjectRoot(), moduleName);
  }

  private String getRepositoryContentOf(TestFile file1) throws Exception {
    GetFileContentOperation operation = GetFileContentOperation.createForFile(file1.getVirtualFile());
    execute(operation);
    return new String(operation.getFileBytes(), StandardCharsets.UTF_8);
  }



  private void checkoutFirstFile() {
    checkoutFile(myFile1);
  }


  private void changeFilesLocally() throws IOException {

    myFile1.changeContentTo(NEW_CONTENT);

    myFile2.changeContentTo(NEW_CONTENT);


    myFileNotInRepository.createInProject();

    assertTrue(myFileNotInRepository.exists());

  }


  private void checkinFirstFile() throws VcsException {

    myVcs.getStandardOperationsProvider().checkinFile(myFile1.getAbsolutePath(), null, null);

    commitTransaction();

  }


}
