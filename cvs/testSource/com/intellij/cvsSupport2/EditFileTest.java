package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.cvsoperations.cvsEdit.EditOperation;
import com.intellij.cvsSupport2.cvsoperations.cvsEdit.EditorInfo;
import com.intellij.cvsSupport2.cvsoperations.cvsEdit.EditorsOperation;
import com.intellij.cvsSupport2.cvsoperations.cvsEdit.UneditOperation;
import com.intellij.cvsSupport2.cvsoperations.cvsWatch.WatchOperation;
import com.intellij.cvsSupport2.cvsoperations.cvsWatch.WatcherInfo;
import com.intellij.cvsSupport2.cvsoperations.cvsWatch.WatchersOperation;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.EnvironmentUtil;
import com.intellij.util.io.ReadOnlyAttributeUtil;
import org.netbeans.lib.cvsclient.command.watch.WatchMode;

import java.io.IOException;
import java.util.List;

/**
 * author: lesya
 */
public class EditFileTest extends CvsTestsWorkingWithImportedProject {
  private TestFile myAnotherTestFile;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    addFile(TEST_FILE);

    watchOn(TEST_FILE);

    //TEST_FILE.deleteFromFileSystem();

    checkoutToAnotherLocation();
    myAnotherTestFile = getTestFileIn(myAnotherLocation);

    assertFalse(myAnotherTestFile.canWrite());
  }

  public void testEdit() throws Exception {
    edit(myAnotherTestFile);
    assertTrue(myAnotherTestFile.canWrite());
  }

  public void testReservedEdit() throws Exception {
    checkoutProject();
    edit(TEST_FILE);

    try {
      edit(myAnotherTestFile);
    } catch (VcsException ex) {
      assertEquals(EditOperation.FILES_BEING_EDITED_EXCEPTION, ex.getMessage());
      assertEquals(myAnotherTestFile.getVirtualFile(), ex.getVirtualFile());
      return;
    }

    fail("Exception expected");
  }

  public void testReservedEditTheSameFile() throws Exception {
    checkoutProject();
    edit(myAnotherTestFile);

    try {
      edit(myAnotherTestFile);
    } catch (VcsException ex) {
      fail("Unexpected exception");
    }
  }

  public void testCommit() throws Exception {
    commitTestFile("content before commitAssertingNoCircularDependency");

    assertFalse(myAnotherTestFile.canWrite());
  }

  public void testUpdate() throws Exception {
    commitTestFile("1");
    commitTestFile("1");
    updateTestFile();
    assertFalse(TEST_FILE.canWrite());

    commitTestFile("1");
    updateTestFile();
    assertFalse(TEST_FILE.canWrite());
  }

  private void commitTestFile(String content) throws Exception {
    edit(myAnotherTestFile);
    assertTrue(myAnotherTestFile.canWrite());
    myAnotherTestFile.changeContentTo(content);
    myVcs.getStandardOperationsProvider().checkinFile(myAnotherTestFile.getAbsolutePath(), null, null);
    commitTransaction();
  }

  public void testUnedit() throws Exception {
    ReadOnlyAttributeUtil.setReadOnlyAttribute(myAnotherTestFile.getAbsolutePath(), true);
    edit(myAnotherTestFile);
    assertTrue(myAnotherTestFile.canWrite());
    myVcs.getStandardOperationsProvider().checkinFile(myAnotherTestFile.getAbsolutePath(), null, null);
    myAnotherTestFile.changeContentTo("content before commitAssertingNoCircularDependency");

    assertTrue(myAnotherTestFile.canWrite());

    unedit(myAnotherTestFile);

    assertFalse(myAnotherTestFile.canWrite());

  }

  public void testUneditCommandShouldRestoreContent_SRC19777() throws Exception {
    String contentBeforeEdit = "before_edit";
    TEST_FILE.changeContentTo(contentBeforeEdit);
    long timeStamp = TEST_FILE.lastModified();
    edit(TEST_FILE);
    TEST_FILE.changeContentTo("after_edir");
    unedit(TEST_FILE);
    assertEquals(contentBeforeEdit, TEST_FILE.getContent());
    assertEquals(timeStamp, TEST_FILE.lastModified());
  }

  public void testEditors() throws Exception {
    checkoutProject();
    edit(myAnotherTestFile);
    EditorsOperation editorsOperation =
      new EditorsOperation(new VirtualFile[]{myAnotherTestFile.getVirtualFile()});
    execute(editorsOperation);
    checkUsers(editorsOperation.getEditors());
  }

  public void testWatch() throws Exception {
    checkoutToAnotherLocation();
    assertFalse(myAnotherTestFile.canWrite());
    watchOff(TEST_FILE);
    checkoutToAnotherLocation();
    assertTrue(myAnotherTestFile.canWrite());
    watchOff(TEST_FILE);
  }

  public void testWatchers() throws Exception {
    watchAdd(myAnotherTestFile);
    WatchersOperation watchersOperation =
      new WatchersOperation(new VirtualFile[]{myAnotherTestFile.getVirtualFile()});
    execute(watchersOperation);
    List<WatcherInfo> watchers = watchersOperation.getWatchers();
    assertEquals(1, watchers.size());
    WatcherInfo watcher = watchers.get(0);
    assertEquals(TestObject.getUser(), watcher.getUser());
    assertEquals(myAnotherTestFile.getName(), watcher.getFile());
    assertEquals("edit, unedit, commit", watcher.getActions());

  }

  public void testSRC19554() {
    EditorInfo.createOn("1\t2\t22 Sep 2003 10:19:30 GMT\t4\t5");
  }

  private void checkUsers(List<EditorInfo> editors) throws IOException {
    assertEquals(1, editors.size());
    EditorInfo editor = editors.get(0);
    assertEquals(TestObject.getUser(), editor.getUserName());
    assertEquals(0, getHostName().compareToIgnoreCase(editor.getHostName()));
    assertEquals(0, myAnotherTestFile.getParentFile().getCanonicalPath().compareToIgnoreCase(editor.getPath()));
  }

  private static String getHostName() {
    return EnvironmentUtil.getValue("COMPUTERNAME");
  }

  private void unedit(TestFile testFile) throws Exception {
    UneditOperation editOperation = new UneditOperation(false);
    editOperation.addFile(testFile.getVirtualFile());
    execute(editOperation);
  }

  private void edit(TestFile testFile) throws Exception {
    EditOperation editOperation = new EditOperation(true);
    editOperation.addFile(testFile.getVirtualFile());
    execute(editOperation);
  }

  private void watchOn(TestFile testFile) throws Exception {
    WatchOperation watchOperation = new WatchOperation(WatchMode.ON);
    watchOperation.addFile(testFile.getVirtualFile());
    execute(watchOperation);
  }

  private void watchAdd(TestFile testFile) throws Exception {
    WatchOperation watchOperation = new WatchOperation(WatchMode.ADD);
    watchOperation.addFile(testFile.getVirtualFile());
    execute(watchOperation);
  }

  private void watchOff(TestFile testFile) throws Exception {
    WatchOperation watchOperation = new WatchOperation(WatchMode.OFF);
    watchOperation.addFile(testFile.getVirtualFile());
    execute(watchOperation);
  }

}
