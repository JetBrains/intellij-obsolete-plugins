package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.cvsoperations.cvsContent.GetFileContentOperation;
import com.intellij.cvsSupport2.cvsoperations.dateOrRevision.RevisionOrDate;

/**
 * author: lesya
 */
public class WorkingWithTemplatesTest extends CvsTestsWorkingWithImportedProject {

  public void test() throws Exception {
    addFile(TEST_FILE);

    refreshFileSystem();

    execute(new GetFileContentOperation(CvsUtil.getCvsLightweightFileForFile(TEST_FILE)
                                        , this, RevisionOrDate.EMPTY));

  }

}
