package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.application.CvsEntriesManager;
import com.intellij.cvsSupport2.application.CvsStorageSupportingDeletionComponent;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PsiTestUtil;

import java.io.IOException;

/**
 * author: lesya
 */
public class EntriesManagerTest extends CvsTestsWorkingWithImportedProject {
  public void testEntriesWillBeDroppedAfterMoveOrCopyDirectory() throws VcsException, IOException {
    CvsStorageSupportingDeletionComponent cvsStorageSupportingDeletionComponent = CvsStorageSupportingDeletionComponent.getInstance(myProject);
    cvsStorageSupportingDeletionComponent.activate();
    try {
      TestDirectory directory = TestDirectory.createInProject("dir");

      refreshFileSystem();

      TestDirectory subDirectory = new TestDirectory(directory, "subDir");
      subDirectory.mkdir();
      TestFile file = new TestFile(subDirectory, "file.txt");


      addFile(directory);
      addFile(subDirectory);
      addFile(file);

      refreshFileSystem();

      addAsContentRoot(directory);

      VirtualFile subDirVF = subDirectory.getVirtualFile();
      String fileName = file.getName();


      assertNotNull(CvsEntriesManager.getInstance().getEntryFor(subDirVF,
                                                                fileName));


      directory.getVirtualFile().rename(this, "newName");

      cvsStorageSupportingDeletionComponent.sync();


      assertNull(CvsEntriesManager.getInstance().getEntryFor(subDirVF,
                                                             fileName));
    }
    finally {
      cvsStorageSupportingDeletionComponent.deactivate();
    }
  }

  private void addAsContentRoot(final TestDirectory directory) {
    PsiTestUtil.addContentRoot(myModule, directory.getVirtualFile());
  }
}
