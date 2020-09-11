package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.cvsstatuses.CvsStatusProvider;
import com.intellij.openapi.vcs.FileStatus;

/**
 * author: lesya
 */
public class RefactoringSupportTest extends CvsTestsWorkingWithImportedProject{

  public void testRenameClassAndCreateInterfaceWithTheSameName() throws Exception {

    TestFile classFile = TestFile.createInProject("Class.java");
    classFile.changeContentTo("class Class {}");

    addFile(classFile);
    classFile.deleteFromFileSystem();
    removeLocally(classFile);
    Thread.sleep(4000);
    refreshFileSystem();

    String interfaceContent = "interface Class{}";
    classFile.changeContentTo(interfaceContent);
    Thread.sleep(4000);
    CvsUtil.restoreFile(classFile.getVirtualFile());

    CvsStatusProvider.getStatus(classFile.getVirtualFile());

    refreshFileSystem();

    assertEquals(interfaceContent, classFile.getContent());
    assertEquals(FileStatus.MODIFIED ,CvsStatusProvider.getStatus(classFile.getVirtualFile()));
  }

}
