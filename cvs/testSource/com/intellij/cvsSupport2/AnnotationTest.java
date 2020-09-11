package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.cvsoperations.cvsAnnotate.Annotation;
import junit.framework.TestCase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class AnnotationTest extends TestCase{
  public void test() throws ParseException {

    checkAnnotation("1.1          (User 04-May-04): some text", "1.1", "User", "04-May-04", "some text");
    checkAnnotation("1.1          (Another User 04-May-04): some text", "1.1",
                    "Another User", "04-May-04", "some text");    
    checkAnnotation("1.1          (Yet Another User Name 04-May-04): some text", "1.1",
                    "Yet Another User Name", "04-May-04", "some text");
  }

  private static void checkAnnotation(String annotationString, String revision, String user, String date, String text) throws ParseException {
    Annotation annotation = Annotation.createOnMessage(annotationString);
    assertEquals(revision, annotation.getRevision());
    assertEquals(user, annotation.getUserName());
    assertEquals(new SimpleDateFormat("dd-MMM-yy", Locale.US).parse(date), annotation.getDate());
    assertEquals(text, Annotation.createMessageOn(annotationString));
  }
}