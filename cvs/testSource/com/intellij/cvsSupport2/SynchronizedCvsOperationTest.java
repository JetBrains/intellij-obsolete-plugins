package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.cvsoperations.cvsAdd.AddFileOperation;
import com.intellij.util.ConcurrencyUtil;
import org.netbeans.lib.cvsclient.command.KeywordSubstitution;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * author: lesya
 */

public class SynchronizedCvsOperationTest extends CvsTestsWorkingWithImportedProject {

  private int myFinishedThreads = 0;

  public void test() throws InterruptedException, IOException {
    ArrayList<File> files = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      File file = new File(TestObject.getProjectDirectory(), "file" + i + ".txt");
      file.createNewFile();
      files.add(file);
    }

    ArrayList<Thread> threads = new ArrayList<>();
    for (final File file : files) {
      threads.add(new Thread(() -> {
        try {
          AddFileOperation operation = new AddFileOperation(KeywordSubstitution.NO_SUBSTITUTION);
          operation.addFile(file.getAbsolutePath());
          execute(operation);
          myFinishedThreads++;
        }
        catch (Exception e) {

        }
      }, "sync cvs") {
      });
    }

    for (Thread thread : threads) {
      thread.start();
    }

    while (myFinishedThreads < threads.size()) Thread.sleep(100);

    for (File file : files) {
      assertTrue(CvsUtil.fileIsLocallyAdded(file));
    }

    ConcurrencyUtil.joinAll(threads);
  }

}
