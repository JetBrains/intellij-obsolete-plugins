package com.intellij.cvsSupport2;

import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.LocalFileSystem;

/**
 * author: lesya
 */
public class LineSeparatorsTest extends CvsTestsWorkingWithImportedProject{

  public void test() throws Exception {
    TEST_FILE.createInProject();
    String content = "1\r2\r3";
    TEST_FILE.changeContentTo(content);
    TEST_FILE.addToVcs(myVcs);
    commitTransaction();

    refreshFileSystem();

    checkoutToAnotherLocation();
    TestFile another = getInAnotherLocation(TEST_FILE);
    String content2 = "3\r4\r5";
    another.changeContentTo(content2);
    myVcs.getStandardOperationsProvider().checkinFile(another.getAbsolutePath(), null, null);
    commitTransaction();

    assertNotNull(ModuleUtilCore.findModuleForFile(LocalFileSystem.getInstance().findFileByIoFile(TEST_FILE), myProject));

    updateFile(TEST_FILE);

    String contentAfterUpdate = TEST_FILE.getContent();
    assertEquals(content2, contentAfterUpdate);
  }

}
