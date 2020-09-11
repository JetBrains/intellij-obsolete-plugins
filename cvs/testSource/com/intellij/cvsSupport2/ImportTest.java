package com.intellij.cvsSupport2;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.testFramework.PsiTestUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;


public class ImportTest extends BaseCvs2TestCase {
  private static final int NUMBER_OF_DIRECTORIES = 10;
  private static final String KEYWORD = "$Date: 2004/10/18 18:26:39 $";
  private String myOldIgnoredFileList;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myOldIgnoredFileList = FileTypeManager.getInstance().getIgnoredFilesList();
  }

  @Override
  protected void tearDown() throws Exception {
    ApplicationManager.getApplication().runWriteAction(() -> FileTypeManager.getInstance().setIgnoredFilesList(myOldIgnoredFileList));
    super.tearDown();
  }

  public void test() throws Exception {
    Collection<String> newFileNames = new ArrayList<>();

    newFileNames.addAll(createTreeIn(TestObject.getProjectDirectory(), "firstTree"));
    newFileNames.addAll(createTreeIn(TestObject.getProjectDirectory(), "secondTree"));

    importProject();

    for (String path : newFileNames) {
      TestFile file = new TestFile(path);
      assertTrue(file.getAbsolutePath() + " should be in repository", file.isInRepository());
    }
  }

  public void testImportExeFile() throws Exception {
    TestFile testFile = new TestFile(TestObject.getProjectDirectory(), "test.exe");
    testFile.createInProject();

    importProject();

    assertTrue(testFile.isInRepository());
  }

  public void testDisableKeywordsSubstitution() throws Exception {
    setIgnoringKeywordsOnServer();
    TestFile testFile = new TestFile(TestObject.getProjectDirectory(), "test.txt");
    testFile.createInProject();
    testFile.changeContentTo(KEYWORD);

    importProject();

    checkoutProject();

    assertTrue(testFile.projectVersionContains(KEYWORD));
  }

  public void obsoletetestImportIgnoresExcludedAndIgnoredFiles() throws Exception {
    TestDirectory excludedFile = new TestDirectory(TestObject.getProjectDirectory(), "excluded");
    excludedFile.createInProject();
    TestFile ignoredFile = new TestFile(TestObject.getProjectDirectory(), "ignored.txt");
    ignoredFile.createInProject();
    TestFile file = new TestFile(TestObject.getProjectDirectory(), "file.txt");
    file.createInProject();

    exclude(excludedFile);
    ignore(ignoredFile);

    importProject();

    assertFalse(excludedFile.isInRepository());
    assertFalse(ignoredFile.isInRepository());
    assertTrue(file.isInRepository());
  }

  private static void ignore(final TestFile ignoredFile) {
    ApplicationManager.getApplication().runWriteAction(() -> FileTypeManager.getInstance().setIgnoredFilesList(ignoredFile.getName()));
  }

  private void exclude(final TestObject excludedFile) {
    PsiTestUtil.addContentRoot(myModule, new TestDirectory(excludedFile.getParentFile().getAbsolutePath()).getVirtualFile());
    PsiTestUtil.addExcludedRoot(myModule, excludedFile.getVirtualFile());
  }

  private void setIgnoringKeywordsOnServer() throws Exception {
    appendStringsToFileInCvsRoot("cvswrappers", new String[]{"* -k 'o'"});
  }

  private static Collection<String> createTreeIn(File parentDirectory, String treeName) throws IOException {
    Collection<String> result = new ArrayList<>();
    for (int i = 0; i < NUMBER_OF_DIRECTORIES; i++){
      File dir = new File(parentDirectory, treeName + i);
      dir.mkdir();
      TestFile file = new TestFile(dir, "file.txt");
      result.add(file.getAbsolutePath());
      file.changeContentTo(NEW_CONTENT);
      parentDirectory = dir;
    }
    return result;
  }
}
