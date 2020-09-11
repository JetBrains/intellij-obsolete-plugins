package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.cvsoperations.cvsLog.LocalPathIndifferentLogOperation;
import org.netbeans.lib.cvsclient.command.CommandException;

/**
 * author: lesya
 */
public class HistoryForEmptyFileTest extends CvsTestsWorkingWithImportedProject{
  public void test() throws Exception {

    TEST_FILE.createNewFile();
    TEST_FILE.addToVcs(myVcs);
    commitTransaction();

    LocalPathIndifferentLogOperation operation = new LocalPathIndifferentLogOperation(TEST_FILE);
    try{
    execute(operation);
    } catch (CommandException ex){
      assertTrue("Unexpected Exception", false);
    }
  }
}
