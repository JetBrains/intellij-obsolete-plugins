package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.cvsExecution.ModalityContextImpl;
import com.intellij.cvsSupport2.cvshandlers.CommandCvsHandler;
import com.intellij.cvsSupport2.cvshandlers.CvsHandler;

import java.io.File;

/**
 * author: lesya
 */
public class MakeNewFileReadOnlyTest extends CvsTestsWorkingWithImportedProject {

  public void test() throws Exception {
    addFile(TEST_FILE);

    TestObject.getProjectDirectory().deleteFromFileSystem();
    checkoutWithMakeNewFilesReadOnlyOption();

    assertFalse(TEST_FILE.canWrite());

    TestObject.getProjectDirectory().deleteFromFileSystem();
    checkoutProject();
    assertTrue(TEST_FILE.canWrite());
  }

  private void checkoutWithMakeNewFilesReadOnlyOption() {
    deleteDirectory(new File(getProjectRoot(), getModuleName()));
    CvsHandler checkoutHandler = CommandCvsHandler.createCheckoutHandler(this,
                                                                         new String[]{getModuleName()},
                                                                         getProjectRoot(),
                                                                         false, true, null);
    checkoutHandler.login(myProject);
    checkoutHandler.internalRun(myProject, ModalityContextImpl.NON_MODAL, false);
  }

}
