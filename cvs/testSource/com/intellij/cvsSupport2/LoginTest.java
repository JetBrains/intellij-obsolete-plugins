package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.connections.CvsEnvironment;
import com.intellij.cvsSupport2.cvsoperations.common.LoginPerformer;
import com.intellij.cvsSupport2.cvsoperations.cvsAdd.AddFilesOperation;

import java.util.HashSet;
import java.util.LinkedList;

/**
 * author: lesya
 */

public class LoginTest extends CvsTestsWorkingWithImportedProject {

  public void test() throws Exception {

    AddFilesOperation addFilesOperation = new AddFilesOperation();

    for (int i = 0; i < 10; i++){
      TestFile file = TestFile.createInProject("file" + i + ".txt");
      file.createInProject();
      refreshFileSystem();
      addFilesOperation.addFile(file.getVirtualFile(), null);
    }

    myLogins = 0;

    final HashSet<CvsEnvironment> roots = new HashSet<>();
    addFilesOperation.appendSelfCvsRootProvider(roots);
    final LinkedList<CvsEnvironment> asEnvList = new LinkedList<>();
    asEnvList.addAll(roots);
    final LoginPerformer performer = new LoginPerformer(myProject, asEnvList, e -> fail());
    performer.loginAll();

    assertEquals(1, myLogins);
  }

}
