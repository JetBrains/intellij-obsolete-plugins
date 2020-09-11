package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.cvsoperations.cvsAnnotate.AnnotateOperation;
import com.intellij.cvsSupport2.cvsoperations.cvsAnnotate.Annotation;

import java.util.Date;

/**
 * author: lesya
 */
public class AnnotationCommandTest extends CvsTestsWorkingWithImportedProject{

  public void test() throws Exception {
    String content = "1\n2\n3\n\n";

    TEST_FILE.createInProject();
    TEST_FILE.changeContentTo(content);
    TEST_FILE.addToVcs(myVcs);

    Date now = new Date();

    commitTransaction();

    refreshFileSystem();

    AnnotateOperation annotateOperation =  AnnotateOperation.createForFile(TEST_FILE);
    execute(annotateOperation);
    Annotation[] lineAnnotations = annotateOperation.getLineAnnotations();
    assertEquals(4, lineAnnotations.length);

    checkAnnotation(lineAnnotations[0], "1.1", myConfig.USER,  now);

    assertEquals(content, annotateOperation.getContent());

  }

  private static void checkAnnotation(Annotation lineAnnotation,
                                      String revision, String user, Date now) {
    assertEquals(user, lineAnnotation.getUserName());
    assertEquals(revision, lineAnnotation.getRevision());
    long nowTime = now.getTime();
    long datesDelta = nowTime - lineAnnotation.getDate().getTime();
    assertTrue(nowTime + "; " + lineAnnotation.getDate().getTime() + ";" + datesDelta, datesDelta < 2000 * 60 * 60 * 24);
  }

}
