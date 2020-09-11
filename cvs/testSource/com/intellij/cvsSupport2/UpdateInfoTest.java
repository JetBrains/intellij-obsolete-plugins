package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.actions.update.UpdateSettings;
import com.intellij.cvsSupport2.config.CvsConfiguration;
import com.intellij.cvsSupport2.config.DateOrRevisionSettings;
import com.intellij.cvsSupport2.cvshandlers.CvsUpdatePolicy;
import com.intellij.cvsSupport2.cvsoperations.cvsRemove.RemoveFilesOperation;
import com.intellij.cvsSupport2.cvsstatuses.CvsStatusProvider;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.update.FileGroup;
import com.intellij.openapi.vcs.update.UpdatedFiles;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.netbeans.lib.cvsclient.command.KeywordSubstitution;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * author: lesya
 */
public class UpdateInfoTest extends CvsTestsWorkingWithImportedProject {
  private final TestFile myUpdated = TestFile.createInProject("updated.txt");
  private final TestFile myNotChanged = TestFile.createInProject("notChanged.txt");
  private final TestFile myCreated = TestFile.createInProject("created.txt");
  private final TestFile myRemovedFromServer = TestFile.createInProject("removedFromServer.txt");
  private final TestFile myUnknown = TestFile.createInProject("unknown.txt");
  private final TestFile myIgnored = TestFile.createInProject("ignored.txt");
  private final TestFile myModified = TestFile.createInProject("modified.txt");
  private final TestFile myLocallyAdded = TestFile.createInProject("locallyAdded.txt");
  private final TestFile myLocallyRemoved = TestFile.createInProject("locallyRemoved.txt");
  private final TestFile myMergedWithConflict = TestFile.createInProject("mergedWithConflict.txt");
  private final TestFile myMerged = TestFile.createInProject("merged.txt");
  private final TestFile myRemovedFromServerConflict = TestFile.createInProject("myRemovedFromServerConflict.txt");
  private final TestFile myRemovedLocallyConflict = TestFile.createInProject("myRemovedLocallyConflict.txt");
  private final TestFile myRemovedFromFileSystem = TestFile.createInProject("removedFromFileSystem.txt");
  private final TestFile myBinaryShouldBeMerged = TestFile.createInProject("binaryShouldBeMerged.txt");
  private static final String BRANCH_NAME = "branch";

  public void test() throws Exception {
    addFiles();
    makeChanges(null);
    UpdatedFiles updatedFiles = updateProject();

    refreshFileSystem();

    checkResults(updatedFiles);
    checkResultsAfterMakeChanges(updatedFiles);
  }

  public void testUpdateDoNotMakeChanges() throws Exception {
    addFiles();
    makeChanges(null);
    assertEquals(FileStatus.MODIFIED,
                 CvsStatusProvider.getStatus(myNotChanged.getVirtualFile()));

    myUpdateSettings = UpdateSettings.DONT_MAKE_ANY_CHANGES;
    UpdatedFiles updatedFiles = updateProject();

    refreshFileSystem();

    checkResults(updatedFiles);
    checkResultsAfterDontMakeChanges(updatedFiles);

    FileStatusManager.getInstance(myProject).fileStatusesChanged();

    //assertEquals(FileStatus.NOT_CHANGED,
    //             CvsStatusProvider.getStatus(myNotChanged.getVirtualFile()));
  }

  public void testCreatedBySecondParty() throws Exception {
    TestFile createdBySecondParty = TestFile.createInProject("file.txt");
    addLocally(createdBySecondParty);
    checkoutToAnotherLocation();
    createOnServer(createdBySecondParty);
    UpdatedFiles updatedFiles = updateProject();

    refreshFileSystem();

    checkGroupContainsOf(updatedFiles.getGroupById(CvsUpdatePolicy.CREATED_BY_SECOND_PARTY_ID), createdBySecondParty, FileStatus.ADDED);
  }

  public void testMergeUnmodifiedFileWithBranch() throws Exception {
    TestFile testFile = TestFile.createInProject("test.txt");
    String initialContent = "1111\n2222\n3333\n";
    testFile.changeContentTo(initialContent);
    refreshFileSystem();

    testFile.addToVcs(myVcs);
    refreshFileSystem();
    commitTransaction("");
    refreshFileSystem();

    createBranch(BRANCH_NAME);

    setWorkingTag(BRANCH_NAME);

    checkoutToAnotherLocation();

    TestFile file = getInAnotherLocation(testFile);
    String mergedContent = "1111\n2222\n4444\n3333\n";
    file.changeContentTo(mergedContent);
    refreshFileSystem();
    checkinFile(file);

    myConfiguration.MERGING_MODE = CvsConfiguration.MERGE_WITH_BRANCH;
    myConfiguration.MERGE_WITH_BRANCH1_NAME = BRANCH_NAME;
    setWorkingTag(null);

    assertEquals(initialContent, testFile.getContent());
    UpdatedFiles updatedFiles = updateProject();
    assertEquals(mergedContent, testFile.getContent());
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.MERGED_ID), testFile, FileStatus.MERGE);
  }

  public void testMergeModifiedFileWithBranch() throws Exception {
    TestFile testFile = TestFile.createInProject("test.txt");
    String initialContent = "1111\n2222\n3333\n";
    testFile.changeContentTo(initialContent);
    refreshFileSystem();

    testFile.addToVcs(myVcs);
    refreshFileSystem();
    commitTransaction("");
    refreshFileSystem();

    createBranch(BRANCH_NAME);

    setWorkingTag(BRANCH_NAME);

    checkoutToAnotherLocation();

    TestFile file = getInAnotherLocation(testFile);
    String mergedContent = "1111\n2222\n4444\n3333\n";
    file.changeContentTo(mergedContent);
    refreshFileSystem();
    checkinFile(file);

    myConfiguration.MERGING_MODE = CvsConfiguration.MERGE_WITH_BRANCH;
    myConfiguration.MERGE_WITH_BRANCH1_NAME = BRANCH_NAME;
    setWorkingTag(null);

    assertEquals(initialContent, testFile.getContent());
    Thread.sleep(1000);
    testFile.changeContentTo("0000\n1111\n2222\n3333\n");
    UpdatedFiles updatedFiles = updateProject();
    assertEquals("0000\n1111\n2222\n4444\n3333\n", testFile.getContent());
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.MERGED_ID), testFile, FileStatus.MERGE);
  }

  public void testMergeCommittedFileWithBranch() throws Exception {
    TestFile testFile = TestFile.createInProject("test.txt");
    String initialContent = "1111\n2222\n3333\n";
    testFile.changeContentTo(initialContent);
    refreshFileSystem();

    testFile.addToVcs(myVcs);
    refreshFileSystem();
    commitTransaction("");
    refreshFileSystem();

    createBranch(BRANCH_NAME);

    String committedContent = "0000\n1111\n2222\n4444\n3333\n";
    testFile.changeContentTo(committedContent);
    checkinFile(testFile);

    setWorkingTag(BRANCH_NAME);

    checkoutToAnotherLocation();

    TestFile file = getInAnotherLocation(testFile);
    String mergedContent = "1111\n2222\n4444\n3333\n";
    file.changeContentTo(mergedContent);
    refreshFileSystem();
    checkinFile(file);

    myConfiguration.MERGING_MODE = CvsConfiguration.MERGE_WITH_BRANCH;
    myConfiguration.MERGE_WITH_BRANCH1_NAME = BRANCH_NAME;
    setWorkingTag(null);

    assertEquals(committedContent, testFile.getContent());
    UpdatedFiles updatedFiles = updateProject();
    assertEquals("0000\n1111\n2222\n4444\n3333\n", testFile.getContent());
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.MERGED_ID), testFile, FileStatus.MERGE);
  }

  public void testMergeTwoBranchesIntoThirdOne() throws Exception {
    UpdatedFiles updatedFiles = performMergeBranchesTest("branch1", "branch2", "branch3", "branch2");
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.MERGED_ID), new TestFile[]{myUpdated}, FileStatus.MERGE);
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.CREATED_ID), myCreated, FileStatus.ADDED);
  }

  public void testMergeTwoBranches() throws Exception {
    UpdatedFiles updatedFiles = performMergeBranchesTest("branch1", "branch2", null, "branch2");
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.MERGED_ID), new TestFile[]{myUpdated}, FileStatus.MERGE);
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.CREATED_ID), myCreated, FileStatus.ADDED);
  }

  public void testUpdateIntoAnotherBranch() throws Exception {
    UpdatedFiles updatedFiles = performMergeBranchesTest(null, null, "branch3", "branch3");
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.UPDATED_ID), new TestFile[]{myUpdated}, FileStatus.NOT_CHANGED);
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.CREATED_ID), myCreated, FileStatus.NOT_CHANGED);
  }

  public void testGetMergeInformationForModifiedFile() throws Exception {
    addFile(myMergedWithConflict);

    createBranch("branch1");
    createBranch("branch2");
    createBranch("branch3");
    createBranch("branch4");

    changeContentInBranch("branch1", myMergedWithConflict);
    changeContentInBranch("branch2", myMergedWithConflict);
    changeContentInBranch("branch3", myMergedWithConflict);
    changeContentInBranch("branch4", myMergedWithConflict);

    myConfiguration.UPDATE_DATE_OR_REVISION_SETTINGS.USE_BRANCH = true;
    myConfiguration.UPDATE_DATE_OR_REVISION_SETTINGS.BRANCH = "branch3";

    updateProject();

    myConfiguration.MERGING_MODE = CvsConfiguration.MERGE_TWO_BRANCHES;
    myConfiguration.MERGE_WITH_BRANCH1_NAME = "branch1";
    myConfiguration.MERGE_WITH_BRANCH2_NAME = "branch2";
    myConfiguration.UPDATE_DATE_OR_REVISION_SETTINGS.USE_BRANCH = true;
    myConfiguration.UPDATE_DATE_OR_REVISION_SETTINGS.BRANCH = "branch4";

    Thread.sleep(3000);
    myMergedWithConflict.changeContentTo(myMergedWithConflict.getContent() + "(changed content)");

    updateProject();

    List<String> versions = CvsUtil.getAllRevisionsForFile(myMergedWithConflict.getVirtualFile());
    assertEquals(Arrays.asList("1.1.6.1", "1.1.6.1", "1.1.8.1", "1.1.2.1", "1.1.4.1"),
                 versions);

    assertEquals(new String(CvsUtil.getStoredContentForFile(myMergedWithConflict.getVirtualFile()), StandardCharsets.UTF_8),
                 "content from\n branch3\n(changed content)");
  }

  public void testGetMergeInformationForNonModifiedFile() throws Exception {
    addFile(myMergedWithConflict);

    createBranch("branch1");
    createBranch("branch2");
    createBranch("branch3");
    createBranch("branch4");

    changeContentInBranch("branch1", myMergedWithConflict);
    changeContentInBranch("branch2", myMergedWithConflict);
    changeContentInBranch("branch3", myMergedWithConflict);
    changeContentInBranch("branch4", myMergedWithConflict);

    myConfiguration.UPDATE_DATE_OR_REVISION_SETTINGS.USE_BRANCH = true;
    myConfiguration.UPDATE_DATE_OR_REVISION_SETTINGS.BRANCH = "branch3";

    updateProject();

    myConfiguration.MERGING_MODE = CvsConfiguration.MERGE_TWO_BRANCHES;
    myConfiguration.MERGE_WITH_BRANCH1_NAME = "branch1";
    myConfiguration.MERGE_WITH_BRANCH2_NAME = "branch2";
    myConfiguration.UPDATE_DATE_OR_REVISION_SETTINGS.USE_BRANCH = true;
    myConfiguration.UPDATE_DATE_OR_REVISION_SETTINGS.BRANCH = "branch4";

    updateProject();
  }

  public void testSimpleBranch() throws Exception {
    addFile(myMergedWithConflict);

    createBranch("branch1");

    changeContentInBranch("branch1", myMergedWithConflict);

    myConfiguration.MERGING_MODE = CvsConfiguration.MERGE_WITH_BRANCH;
    myConfiguration.MERGE_WITH_BRANCH1_NAME = "branch1";

    updateProject();

    List<String> versions = CvsUtil.getAllRevisionsForFile(myMergedWithConflict.getVirtualFile());
    assertEquals(Arrays.asList("1.1", "1.1", "1.1.2.1"),
                 versions);
  }

  public void testSimpleBranchForModifiedFile() throws Exception {
    addFile(myMergedWithConflict);

    createBranch("branch1");

    changeContentInBranch("branch1", myMergedWithConflict);

    myConfiguration.MERGING_MODE = CvsConfiguration.MERGE_WITH_BRANCH;
    myConfiguration.MERGE_WITH_BRANCH1_NAME = "branch1";

    Thread.sleep(3000);
    myMergedWithConflict.changeContentTo(myMergedWithConflict.getContent() + "(changed content)");

    updateProject();

    List<String> versions = CvsUtil.getAllRevisionsForFile(myMergedWithConflict.getVirtualFile());
    assertEquals(Arrays.asList("1.1", "1.1", "1.1.2.1"),
                 versions);
  }

  private void changeContentInBranch(String branchName, TestFile fileToChangeContentIn) throws Exception {
    setWorkingTag(branchName);
    checkoutToAnotherLocation();
    TestFile file = getInAnotherLocation(fileToChangeContentIn);
    file.changeContentTo("content from\n " + branchName + "\n");
    refreshFileSystem();
    assertNotNull(LocalFileSystem.getInstance().findFileByIoFile(file));
    checkinFile(file);
  }

  private UpdatedFiles performMergeBranchesTest(String branch1, String branch2, String branch3, String branchToMakeChangesIn) throws Exception {
    addFile(myRemovedFromServerConflict);
    addFile(myUpdated);
    addFile(myNotChanged);
    addFile(myRemovedFromServer);
    addFile(myModified);
    addFile(myMergedWithConflict);
    //addFile(myMerged);

    //addFile(myRemovedLocallyConflict);
    addFile(myLocallyRemoved);
    addFile(myRemovedFromFileSystem);
    addFile(myBinaryShouldBeMerged, KeywordSubstitution.BINARY);


    createBranch(branch1);
    createBranch(branch2);
    createBranch(branch3);

    myIgnored.createInProject();
    setWorkingTag(branchToMakeChangesIn);

    checkoutToAnotherLocation();

    checkinFile(myUpdated);

    changeOnServer(myUpdated);
    removeOnServer(myRemovedFromServer);

    // //changeOnServerAndRemoveLocally(myRemovedLocallyConflict);

    modify(myModified);
    changeContentToProvideMergingWithConflict(myMergedWithConflict);
    //changeContentToProvideMergingWithoutConflict(myMerged);
    changeContentToProvideMergingWithoutConflict(myBinaryShouldBeMerged);
    createInProject(myUnknown);
    createOnServer(myCreated);
    modifyAndRemoveFromServer(myRemovedFromServerConflict);
    addLocally(myLocallyAdded);
    removeLocally(myLocallyRemoved);
    LOG.assertTrue(myRemovedFromFileSystem.delete());

    refreshFileSystem();

    CvsUtil.ignoreFile(myIgnored.getVirtualFile());

    refreshFileSystem();

    CvsUtil.ignoreFile(TestFile.createInProject(".cvsignore").getVirtualFile());

    myNotChanged.setLastModified(myNotChanged.lastModified() + 10000);
    refreshFileSystem();


    setWorkingTag(null);

    if (branch1 == null){
      myConfiguration.MERGING_MODE = CvsConfiguration.DO_NOT_MERGE;
    } else if (branch2 == null) {
      myConfiguration.MERGING_MODE = CvsConfiguration.MERGE_WITH_BRANCH;
    } else {
      myConfiguration.MERGING_MODE = CvsConfiguration.MERGE_TWO_BRANCHES;
    }

    myConfiguration.MERGE_WITH_BRANCH1_NAME = branch1;
    myConfiguration.MERGE_WITH_BRANCH2_NAME = branch2;
    myConfiguration.UPDATE_DATE_OR_REVISION_SETTINGS = new DateOrRevisionSettings();
    myConfiguration.UPDATE_DATE_OR_REVISION_SETTINGS.USE_BRANCH = true;
    myConfiguration.UPDATE_DATE_OR_REVISION_SETTINGS.BRANCH = branch3;

    UpdatedFiles updatedFiles = updateProject();

    refreshFileSystem();

    checkGroupContainsOf(updatedFiles.getGroupById(CvsUpdatePolicy.MODIFIED_REMOVED_FROM_SERVER_ID), myRemovedFromServerConflict,
                         FileStatus.MODIFIED);
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.REMOVED_FROM_REPOSITORY_ID), myRemovedFromServer, null);
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.MERGED_WITH_CONFLICT_ID), myMergedWithConflict, FileStatus.MERGED_WITH_CONFLICTS);
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.MODIFIED_ID), myModified, FileStatus.MODIFIED);
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.UNKNOWN_ID), myUnknown, FileStatus.UNKNOWN);
    //checkGroupContainsOf(updatedFiles.getGroupById(CvsUpdatePolicy.LOCALLY_REMOVED_MODIFIED_ON_SERVER_ID), myRemovedLocallyConflict,
    //                     FileStatus.DELETED);
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.LOCALLY_ADDED_ID), myLocallyAdded, FileStatus.ADDED);
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.LOCALLY_REMOVED_ID), myLocallyRemoved, FileStatus.DELETED);
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.RESTORED_ID), myRemovedFromFileSystem, FileStatus.NOT_CHANGED);
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.MERGED_WITH_CONFLICT_ID), myMergedWithConflict,
                         FileStatus.MERGED_WITH_CONFLICTS);
    checkGroupContainsOf(updatedFiles.getGroupById(CvsUpdatePolicy.BINARY_MERGED_ID), myBinaryShouldBeMerged,
                         FileStatus.MERGE);
    return updatedFiles;
  }

  private void makeChanges(String tagName) throws Exception {
    myIgnored.createInProject();
    setWorkingTag(tagName);

    checkoutToAnotherLocation();

    changeOnServer(myUpdated);
    changeOnServerAndRemoveLocally(myRemovedLocallyConflict);
    removeOnServer(myRemovedFromServer);
    modify(myModified);
    changeContentToProvideMergingWithConflict(myMergedWithConflict);
    changeContentToProvideMergingWithoutConflict(myMerged);
    changeContentToProvideMergingWithoutConflict(myBinaryShouldBeMerged);
    createInProject(myUnknown);
    createOnServer(myCreated);
    modifyAndRemoveFromServer(myRemovedFromServerConflict);
    addLocally(myLocallyAdded);
    removeLocally(myLocallyRemoved);
    LOG.assertTrue(myRemovedFromFileSystem.delete());

    refreshFileSystem();

    CvsUtil.ignoreFile(myIgnored.getVirtualFile());

    refreshFileSystem();

    CvsUtil.ignoreFile(TestFile.createInProject(".cvsignore").getVirtualFile());

    myNotChanged.setLastModified(myNotChanged.lastModified() + 10000);
    refreshFileSystem();
  }

  private void addFiles() throws IOException, VcsException {
    addFile(myUpdated);
    addFile(myNotChanged);
    addFile(myRemovedFromServer);
    addFile(myModified);
    addFile(myMergedWithConflict);
    addFile(myMerged);
    addFile(myRemovedFromServerConflict);
    addFile(myRemovedLocallyConflict);
    addFile(myLocallyRemoved);
    addFile(myRemovedFromFileSystem);
    addFile(myBinaryShouldBeMerged, KeywordSubstitution.BINARY);
  }

  private void changeOnServerAndRemoveLocally(TestFile removedLocallyConflict) throws Exception {
    changeOnServer(removedLocallyConflict);
    RemoveFilesOperation operation = new RemoveFilesOperation();
    operation.addFile(removedLocallyConflict.getAbsolutePath());
    removedLocallyConflict.deleteFromFileSystem();
    execute(operation);
  }


  private void checkResultsAfterMakeChanges(UpdatedFiles updatedFiles) throws IOException {
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.REMOVED_FROM_REPOSITORY_ID), myRemovedFromServer, FileStatus.UNKNOWN);
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.MERGED_ID), myMerged, FileStatus.MERGE);
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.MERGED_WITH_CONFLICT_ID), myMergedWithConflict,
                         FileStatus.MERGED_WITH_CONFLICTS);
    checkGroupContainsOf(updatedFiles.getGroupById(CvsUpdatePolicy.BINARY_MERGED_ID), myBinaryShouldBeMerged,
                         FileStatus.MERGE);

  }

  private void checkResultsAfterDontMakeChanges(UpdatedFiles updatedFiles) throws IOException {
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.REMOVED_FROM_REPOSITORY_ID), myRemovedFromServer, FileStatus.NOT_CHANGED);
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.MERGED_ID), myMerged, FileStatus.MODIFIED);
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.MERGED_WITH_CONFLICT_ID), myMergedWithConflict, FileStatus.MODIFIED);
  }

  private void checkResults(UpdatedFiles updatedFiles) throws IOException {
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.CREATED_ID), myCreated, FileStatus.NOT_CHANGED);
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.UPDATED_ID), myUpdated, FileStatus.NOT_CHANGED);
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.MODIFIED_ID), myModified, FileStatus.MODIFIED);
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.UNKNOWN_ID), myUnknown, FileStatus.UNKNOWN);
    checkGroupContainsOf(updatedFiles.getGroupById(CvsUpdatePolicy.MODIFIED_REMOVED_FROM_SERVER_ID), myRemovedFromServerConflict,
                         FileStatus.MODIFIED);
    checkGroupContainsOf(updatedFiles.getGroupById(CvsUpdatePolicy.LOCALLY_REMOVED_MODIFIED_ON_SERVER_ID), myRemovedLocallyConflict,
                         FileStatus.DELETED);
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.LOCALLY_ADDED_ID), myLocallyAdded, FileStatus.ADDED);
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.LOCALLY_REMOVED_ID), myLocallyRemoved, FileStatus.DELETED);
    checkGroupContainsOf(updatedFiles.getGroupById(FileGroup.RESTORED_ID), myRemovedFromFileSystem, FileStatus.NOT_CHANGED);
  }

  private void modifyAndRemoveFromServer(TestFile removedFromServerConflict) throws VcsException, IOException {
    TestFile file = getInAnotherLocation(removedFromServerConflict);
    file.deleteFromFileSystem();
    myVcs.getStandardOperationsProvider().removeFile(file.getAbsolutePath(), null, null);
    commitTransaction();

    removedFromServerConflict.changeContentTo("111");
    removedFromServerConflict.setLastModified(10000);
  }

  private void createOnServer(TestFile created) throws VcsException, IOException {
    TestFile file = getInAnotherLocation(created);
    file.createInProject();
    file.addToVcs(myVcs);
    commitTransaction();
  }

  private static void createInProject(TestFile unknown) throws IOException {
    unknown.createInProject();
  }

  private void changeContentToProvideMergingWithoutConflict(TestFile merged) throws Exception {
    String beforeMerge = "1111\n2222\n3333\n4444\n5555\n";

    TestFile file = getInAnotherLocation(merged);
    file.changeContentTo(beforeMerge);
    checkinFile(file);

    updateFile(merged);

    file.changeContentTo(beforeMerge + "6666\n");
    checkinFile(file);

    merged.changeContentTo("2222\n3333\n4444\n5555\n6666\n");
    merged.setLastModified(10000);
  }

  private void changeContentToProvideMergingWithConflict(TestFile mergedWithConflict) throws IOException, VcsException {
    TestFile file = getInAnotherLocation(mergedWithConflict);
    file.changeContentTo("another content");
    checkinFile(file);
    mergedWithConflict.changeContentTo("local content");
  }

  private static void modify(TestFile modified) throws IOException{
    modified.changeContentTo("modified");
    modified.setLastModified(10000);
  }

  private void changeOnServer(TestFile updated) throws Exception {
    TestFile file = getInAnotherLocation(updated);
    file.changeContentTo("another content");
    checkinFile(file);
  }

  private void checkinFile(TestFile file) throws VcsException {
    myVcs.getStandardOperationsProvider().checkinFile(file.getAbsolutePath(), null, null);
    commitTransaction();
  }

  private UpdatedFiles updateProject() throws Exception {
    return updateProjectDirectory();
  }

  private static void checkGroupContainsOf(FileGroup group, TestFile[] files, FileStatus status) throws IOException {
    Collection filesFromGroup = group.getFiles();
    assertEquals(filesFromGroup.toString(), files.length, filesFromGroup.size());
    for (TestFile file : files) {
      assertTrue(filesFromGroup.contains(file.getCanonicalPath()));
      VirtualFile virtualFile = file.getVirtualFile();
      if (virtualFile != null) {
        assertEquals(status, CvsStatusProvider.getStatus(virtualFile));
      }
    }

  }

  private static void checkGroupContainsOf(FileGroup group, TestFile file, FileStatus status) throws IOException {
    checkGroupContainsOf(group, new TestFile[]{file}, status);
  }

}
