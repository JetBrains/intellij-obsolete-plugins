package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.cvsExecution.CvsOperationExecutor;
import com.intellij.cvsSupport2.cvsExecution.CvsOperationExecutorCallback;
import com.intellij.cvsSupport2.cvshandlers.CommandCvsHandler;
import com.intellij.cvsSupport2.cvshandlers.CvsHandler;
import com.intellij.cvsSupport2.cvsoperations.cvsAdd.AddFilesOperation;
import com.intellij.cvsSupport2.cvsoperations.cvsRemove.RemoveFilesOperation;
import com.intellij.cvsSupport2.cvsoperations.cvsUpdate.UpdateOperation;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.vcs.VcsException;
import org.netbeans.lib.cvsclient.command.KeywordSubstitution;
import org.netbeans.lib.cvsclient.command.tag.TagCommand;

import java.io.File;

public abstract class CvsTestsWorkingWithImportedProject extends BaseCvs2TestCase {

  public CvsTestsWorkingWithImportedProject() {
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    importAndCheckoutProject();
  }

  protected void updateTestFile() throws Exception {
    updateFile(TEST_FILE);
  }

  protected void updateFile(TestFile file) throws Exception {
    execute(new UpdateOperation(createArrayOn(file.getVirtualFile()), getUpdateSettings(), myProject));
  }

  protected TestFile getInAnotherLocation(File fileInBranch) {
    return getFileIn(myAnotherLocation, fileInBranch);
  }

  protected void addModule(String moduleName, String directory) throws Exception {
    createModules(new String[]{moduleName + " " + directory});
  }

  protected void addModules(String[] moduleNames) throws Exception {
    createModules(moduleStringsFor(moduleNames));
  }

  private void createModules(String[] modules) throws Exception {
    appendStringsToFileInCvsRoot("modules", modules);
  }

  private static String[] moduleStringsFor(String[] moduleNames) {
    String[] stringsToUppend = new String[moduleNames.length];

    for (int i = 0; i < moduleNames.length; i++) {
      stringsToUppend[i] = moduleNames[i] + " " + getModuleName();
    }
    return stringsToUppend;
  }

  protected void removeOnServer(TestFile removedFromServer) throws VcsException {
    TestFile file = getInAnotherLocation(removedFromServer);
    file.delete();
    myVcs.getStandardOperationsProvider().removeFile(file.getAbsolutePath(), null, null);
    commitTransaction();
  }

  protected void checkoutFile(TestObject file) {
    CvsHandler handler = CommandCvsHandler
      .createCheckoutFileHandler(createArrayOn(file.getVirtualFile()), myConfiguration, PerformInBackgroundOption.DEAF);
    new CvsOperationExecutor(myProject).performActionSync(handler,
                                                 CvsOperationExecutorCallback.EMPTY);
  }

  protected void addLocally(TestFile file) throws Exception {
    file.createInProject();
    AddFilesOperation addOrUpdateFilesOperation = new AddFilesOperation();
    addOrUpdateFilesOperation.addFile(file.getVirtualFile(), KeywordSubstitution.NO_SUBSTITUTION);
    execute(addOrUpdateFilesOperation);
  }

  protected void removeLocally(TestFile file) throws Exception {
    file.delete();
    RemoveFilesOperation removeFilesOperation = new RemoveFilesOperation();
    removeFilesOperation.addFile(file.getAbsolutePath());
    execute(removeFilesOperation);
  }


  protected void createBranch(String branchName) throws VcsException {
    createBranchOrTag(branchName, true);
  }

  protected void createBranchOrTag(String branchName, boolean createBranch) throws VcsException {
    if (branchName == null) return;
    TagCommand tagCommand = new TagCommand();
    tagCommand.setTag(branchName);
    tagCommand.setMakeBranchTag(createBranch);
    executeCommand(tagCommand, TestObject.getProjectDirectory());
  }

  protected void createTag(String tagName) throws VcsException {
    createBranchOrTag(tagName, true);
  }
}
