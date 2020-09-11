package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.config.CvsApplicationLevelConfiguration;
import com.intellij.openapi.application.ex.PathManagerEx;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.util.ConcurrencyUtil;
import junit.framework.TestCase;
import org.netbeans.lib.cvsclient.admin.EntriesHandler;

import java.io.File;
import java.io.IOException;

/**
 * author: lesya
 */
public class EntriesReadWriteTest extends TestCase{
  private int myCompletedTasks = 0;
  private Throwable myException = null;
  public void test() throws IOException, InterruptedException {
    File testData = new File(PathManagerEx.getTestDataPath());
    File parentDirectory =  FileUtil.createTempDirectory("test", "entries");
    final File directory = new File(parentDirectory, "dir");
    directory.mkdirs();
    File data = new File(testData, "cvs2/Entries/Entries");
    File cvs = new File(directory, "CVS");
    cvs.mkdir();
    final File entriesFile = new File(cvs, "Entries");
    FileUtil.copy(data, entriesFile);

    final Thread[] threads = new Thread[20];



    for (int i = 0; i < threads.length; i++){

      threads[i] =
        new Thread(() -> {
          EntriesHandler handler = new EntriesHandler(new File(directory.getAbsolutePath()));
          try{
          handler.read(CvsApplicationLevelConfiguration.getCharset());
          handler.write(CodeStyleSettingsManager.getInstance().getCurrentSettings().getLineSeparator(),
            CvsApplicationLevelConfiguration.getCharset());
          } catch (Exception ex){
            myException = ex;
          } finally{
           myCompletedTasks++;
          }
        }, "RreadWriteEntries " + i);

    }

    for (Thread thread : threads) {
      thread.start();
    }

    while (myCompletedTasks < threads.length && myException == null){
      Thread.sleep(10);
    }

    if (myException != null){
      myException.printStackTrace();
      fail("Unexpected Exception: \n" + myException.getMessage());
    }

    ConcurrencyUtil.joinAll(threads);
  }
}
