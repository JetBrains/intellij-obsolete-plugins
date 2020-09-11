package com.intellij.cvsSupport2;

import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.vcs.VcsConfiguration;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsShowConfirmationOption;
import com.intellij.openapi.vcs.VcsTestUtil;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManagerImpl;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vcs.ex.ProjectLevelVcsManagerEx;
import com.intellij.testFramework.vcs.AbstractVcsTestCase;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author yole
 */
public class CvsFileOperationsTest extends CvsTestsWorkingWithImportedProject {
  @Override
  protected void invokeTestRunnable(@NotNull Runnable runnable) {
    runnable.run();
  }

  private void enableDeleteAction() {
    final ProjectLevelVcsManagerEx manager = ProjectLevelVcsManagerEx.getInstanceEx(myProject);
    manager.getConfirmation(VcsConfiguration.StandardConfirmation.REMOVE).setValue(VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY);
  }

  private void enableAddAction() {
    final ProjectLevelVcsManagerEx manager = ProjectLevelVcsManagerEx.getInstanceEx(myProject);
    manager.getConfirmation(VcsConfiguration.StandardConfirmation.ADD).setValue(VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY);
  }

  private void enableAddConfirmation() {
    final ProjectLevelVcsManagerEx manager = ProjectLevelVcsManagerEx.getInstanceEx(myProject);
    manager.getConfirmation(VcsConfiguration.StandardConfirmation.ADD).setValue(VcsShowConfirmationOption.Value.SHOW_CONFIRMATION);
  }

  public void testDeleteFile() throws Exception {
    enableDeleteAction();

    TestFile classFile = TestFile.createInProject("Class.java");
    classFile.changeContentTo("class Class {}");
    addFile(classFile);
    commitTransaction();

    VcsTestUtil.deleteFileInCommand(myProject, classFile.getVirtualFile());

    final List<Change> changes = collectAllChanges();
    assertEquals(2, changes.size());
    AbstractVcsTestCase.sortChanges(changes);
    Change c = changes.get(1);
    assertNull(c.getAfterRevision());

    CvsVcs2.getInstance(myProject).getCheckinEnvironment().commit(Collections.singletonList(c), "test");
    assertFalse(classFile.getParentFile().exists());
  }

  private List<Change> collectAllChanges() {
    VcsDirtyScopeManager.getInstance(myProject).markEverythingDirty();
    ChangeListManagerImpl changeListManager = ChangeListManagerImpl.getInstanceImpl(myProject);
    changeListManager.ensureUpToDate();
    return new ArrayList<>(changeListManager.getDefaultChangeList().getChanges());
  }

  public void testDeleteDir() throws Exception {
    enableDeleteAction();

    TestDirectory testDir = addDirAndCommit("A");
    TestFile fileInDir = addFileAndCommit(testDir, "1.txt");

    VcsTestUtil.deleteFileInCommand(myProject, testDir.getVirtualFile());
    assertTrue(testDir.exists());
    assertFalse(fileInDir.exists());

    final List<Change> changes = collectAllChanges();
    assertEquals(2, changes.size());

    CvsVcs2.getInstance(myProject).getCheckinEnvironment().commit(changes, "test");
    assertFalse(testDir.exists());
  }

  public void testRenameDir() throws Exception {
    TestDirectory testDir = addDirAndCommit("A");
    addFileAndCommit(testDir, "1.txt");
    enableAddAction();
    VcsTestUtil.renameFileInCommand(myProject, testDir.getVirtualFile(), "B");

    final File oldDir = new File(TestObject.getProjectDirectory(), "A");
    assertTrue(oldDir.isDirectory());
    assertTrue(new File(oldDir, "CVS").isDirectory());
    assertFalse(new File(oldDir, "1.txt").exists());

    final File newDir = new File(TestObject.getProjectDirectory(), "B");
    assertTrue(newDir.isDirectory());
    assertTrue(new File(newDir, "CVS").isDirectory());
    assertTrue(new File(newDir, "1.txt").exists());
  }

  public void testRenameFile() throws Exception {
    addFileAndCommit(TestObject.getProjectDirectory(), "1.txt");
    TestFile f = new TestFile(TestObject.getProjectDirectory(), "1.txt");
    enableAddAction();
    enableDeleteAction();
    VcsTestUtil.renameFileInCommand(myProject, f.getVirtualFile(), "2.txt");

    final List<Change> changes = collectAllChanges();
    assertEquals(2, changes.size());
  }

  public void testMoveDir() throws Exception {
    TestDirectory testDir = addDirAndCommit("A");
    TestDirectory testDir2 = addDirAndCommit("B");
    TestDirectory testDirChild = addDirAndCommit("child", testDir);
    addFileAndCommit(testDirChild, "1.txt");

    enableAddAction();
    VcsTestUtil.moveFileInCommand(myProject, testDirChild.getVirtualFile(), testDir2.getVirtualFile());

    final File oldDir = new File(testDir, "child");
    assertTrue(oldDir.isDirectory());
    assertTrue(new File(oldDir, "CVS").isDirectory());
    assertFalse(new File(oldDir, "1.txt").exists());

    final File newDir = new File(testDir2, "child");
    assertTrue(newDir.isDirectory());
    assertTrue(new File(newDir, "CVS").isDirectory());
    assertTrue(new File(newDir, "1.txt").exists());
  }

  public void testRenameToDeleted() throws Exception {
    final TestFile f1 = addFileAndCommit(TestObject.getProjectDirectory(), "1.txt");
    final TestFile f2 = addFileAndCommit(TestObject.getProjectDirectory(), "2.txt");

    enableDeleteAction();
    enableAddConfirmation();

    CommandProcessor.getInstance().executeCommand(myProject, new Runnable() {
      @Override
      public void run() {
        try {
          f1.getVirtualFile().delete(this);
          f2.getVirtualFile().rename(this, "1.txt");
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }, "", null);

    final List<Change> changes = collectAllChanges();
    assertEquals(2, changes.size());
    assertTrue(new File(TestObject.getProjectDirectory(), "1.txt").exists());
  }

  private TestDirectory addDirAndCommit(final String name) throws IOException, VcsException {
    return addDirAndCommit(name, TestObject.getProjectDirectory());
  }

  private TestDirectory addDirAndCommit(final String name, final TestDirectory parent) throws IOException, VcsException {
    TestDirectory testDir = new TestDirectory(parent, name);
    addFile(testDir);
    commitTransaction();
    return testDir;
  }

  private TestFile addFileAndCommit(final TestDirectory testDir, final String name) throws IOException, VcsException {
    TestFile fileInDir = new TestFile(testDir, name);
    addFile(fileInDir);
    commitTransaction();
    return fileInDir;
  }
}