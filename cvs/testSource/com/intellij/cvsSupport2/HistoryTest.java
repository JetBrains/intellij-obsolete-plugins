package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.actions.update.UpdateSettings;
import com.intellij.cvsSupport2.cvsoperations.cvsLog.LocalPathIndifferentLogOperation;
import com.intellij.cvsSupport2.cvsoperations.cvsTagOrBranch.BranchOperation;
import com.intellij.cvsSupport2.cvsoperations.cvsUpdate.UpdateOperation;
import com.intellij.cvsSupport2.cvsoperations.dateOrRevision.RevisionOrDate;
import com.intellij.cvsSupport2.cvsoperations.dateOrRevision.SimpleRevision;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import org.netbeans.lib.cvsclient.command.KeywordSubstitution;
import org.netbeans.lib.cvsclient.command.log.LogInformation;
import org.netbeans.lib.cvsclient.command.log.Revision;

import java.io.File;
import java.util.List;

/**
 * author: lesya
 */
public class HistoryTest extends CvsTestsWorkingWithImportedProject {

  public void test() throws Exception {
    doTest(TEST_FILE, null);
  }

  public void testForSeveralFiles() throws Exception {
    TestFile file1 = TestFile.createInProject("file1." + TEST_FILE_EXTENSION);
    TestFile file2 = TestFile.createInProject("file2." + TEST_FILE_EXTENSION);
    doTest(new TestFile[]{file1, file2}, null);
  }

  public void testHistoryForFileFromSubdirectory() throws Exception {
    TestDirectory dir1 = TestDirectory.createInProject("dir");
    addDirectory(dir1);

    TestDirectory dir2 = new TestDirectory(dir1, "dir");
    addDirectory(dir2);


    doTest(new TestFile(dir2, "file.txt"), "message\n message2\n message3\n");

  }

  public void testSrc26736() throws Exception {
    TestDirectory dir1 = TestDirectory.createInProject("dir");
    addDirectory(dir1);

    TestDirectory dir2 = new TestDirectory(dir1, "dir");
    addDirectory(dir2);

    try {
      doTest(new TestFile(dir2, "file.txt"), "=======================\n");
    }
    catch (Throwable npe) {
      npe.printStackTrace();
      assertTrue("Unexpected exception", false);
    }

  }

  public void testCommitMessagesWithSplitterInMessage() throws Exception {
    TestDirectory dir1 = TestDirectory.createInProject("dir");
    addDirectory(dir1);

    TestDirectory dir2 = new TestDirectory(dir1, "dir");
    addDirectory(dir2);

    doTest(new TestFile(dir2, "file.txt"), "aaa\n----------------------------\nbbb\n");
  }


  public void testCommitMessagesWithSplitterInMessage2() throws Exception {
    TestDirectory dir1 = TestDirectory.createInProject("dir");
    addDirectory(dir1);

    TestDirectory dir2 = new TestDirectory(dir1, "dir");
    addDirectory(dir2);

    doTest(new TestFile(dir2, "file.txt"), "----------------------------\n" +
                                           "=======================\n" +
                                           "----------------------------\n" +
                                           "=======================\n");
  }

  public void testCommitMessagesWithSplitterInMessage3() throws Exception {
    TestDirectory dir1 = TestDirectory.createInProject("dir");
    addDirectory(dir1);

    TestDirectory dir2 = new TestDirectory(dir1, "dir");
    addDirectory(dir2);

    doTest(new TestFile(dir2, "file.txt"), "=======================\n" +
                                           "----------------------------\n" +
                                           "=======================\n" +
                                           "----------------------------\n");
  }


  public void testSRC28357() throws Exception {
    doTest(TEST_FILE, "message\nmessage\nmessage\n");
  }

  public void testRevision() throws Exception {
    doTest(TEST_FILE, "revision 3\n");
  }

  private void doTest(TestFile testFile, String commitMessage) throws Exception {
    doTest(new TestFile[]{testFile}, commitMessage);
  }

  private void doTest(TestFile[] testFile, String commitMessage) throws Exception {
    for (TestFile file : testFile) {
      addFile(file, null, commitMessage);
    }

    for (TestFile file : testFile) {
      for (int j = 0; j < 4; j++) {
        createBranch(file, String.valueOf(j + 1));
        checkinFile(file, commitMessage);
      }

    }


    LocalPathIndifferentLogOperation operation = new LocalPathIndifferentLogOperation(testFile);
    execute(operation);
    List<LogInformation> logInformationList = operation.getLogInformationList();
    assertEquals(testFile.length ,logInformationList.size());

    for (final LogInformation logInformation : logInformationList) {
      List revisionList = logInformation.getRevisionList();
      assertEquals(5, revisionList.size());
      if (commitMessage != null) {
        for (final Object aRevisionList : revisionList) {
          Revision revision = (Revision)aRevisionList;
          assertEquals(revision.getNumber(), commitMessage, revision.getMessage());
          if (revision.getNumber().equals("1.1")) {
            String branches = revision.getBranches();
            assertNotNull(branches);
            assertEquals("1.1.2;  1.1.4;  1.1.6;  1.1.8", branches);
          }
        }
      }

    }

  }

  private void createBranch(TestFile testFile, String name) throws Exception {
    FilePath[] files = createArrayOn(testFile.getVirtualFile().getParent());
    final String branchName = "a" + name;
    UpdateOperation operation = new UpdateOperation(new FilePath[0], new UpdateSettings() {
      @Override
      public boolean getPruneEmptyDirectories() {
        return false;
      }

      @Override
      public String getBranch1ToMergeWith() {
        return null;
      }

      @Override
      public String getBranch2ToMergeWith() {
        return null;
      }

      @Override
      public boolean getResetAllSticky() {
        return true;
      }

      @Override
      public boolean getDontMakeAnyChanges() {
        return false;
      }

      @Override
      public boolean getCreateDirectories() {
        return true;
      }

      @Override
      public boolean getCleanCopy() {
        return false;
      }

      @Override
      public RevisionOrDate getRevisionOrDate() {
        return new SimpleRevision("HEAD");
      }

      @Override
      public KeywordSubstitution getKeywordSubstitution() {
        return null;
      }

      @Override
      public boolean getMakeNewFilesReadOnly() {
        return false;
      }
    }, myProject);
    operation.addAllFiles(files);
    execute(operation);
    BranchOperation branchOperation = new BranchOperation(files, branchName, true);
    execute(branchOperation);
    execute(new UpdateOperation(files, branchName, false, myProject));
  }

  private void addDirectory(TestDirectory dir) throws VcsException {
    dir.mkdir();
    myVcs.getStandardOperationsProvider().addDirectory(dir.getParentFile().getAbsolutePath(), dir.getName(), "", null);
    commitTransaction();
  }

  private void checkinFile(File file, String commitMessage) throws VcsException {
    myVcs.getStandardOperationsProvider().checkinFile(file.getAbsolutePath(), null, null);
    refreshFileSystem();
    myVcs.commitTransaction(commitMessage);
  }

}
