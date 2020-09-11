package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.cvsoperations.cvsAdd.AddFilesOperation;
import org.netbeans.lib.cvsclient.command.KeywordSubstitution;

/**
 * author: lesya
 */
public class WorkingWithBinaryFilesTest extends CvsTestsWorkingWithImportedProject{
  private static final String BINARY_CONTENT = "\ncontent\n\r\n";

  public void testCheckout() throws Exception {
    TestFile classFile = new TestFile(TestObject.getProjectDirectory(), "Class.class");
    classFile.changeContentTo(BINARY_CONTENT);

    final AddFilesOperation operation = new AddFilesOperation();
    operation.addFile(classFile.getVirtualFile(), KeywordSubstitution.BINARY);
    execute(operation);

    myVcs.getStandardOperationsProvider().checkinFile(classFile.getAbsolutePath(), null, null);
    commitTransaction();

    checkoutProject();

    assertEquals(BINARY_CONTENT, classFile.getContent());
  }
}
