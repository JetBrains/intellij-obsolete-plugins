package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.history.CvsRevisionNumber;
import junit.framework.TestCase;

import static org.junit.Assert.assertNotEquals;

/**
 * author: lesya
 */
public class CvsRevisionNumberComparingTest extends TestCase{

  public void test(){
    checkRevisionsAreEqual("1.2", "1.2");
    checkLessThen("1.1", "1.6");
    checkGraterThen("1.6", "1.2");
    checkGraterThen("1.1.10", "1.1");
    checkLessThen("1.1", "1.1.10");
    checkLessThen("1.2", "1.11");
  }

  private static void checkGraterThen(String revision1, String revision2) {
    CvsRevisionNumber revNumber1 = new CvsRevisionNumber(revision1);
    CvsRevisionNumber revNumber2 = new CvsRevisionNumber(revision2);

    assertEquals(1, revNumber1.compareTo(revNumber2));
    assertNotEquals(revNumber1, revNumber2);
  }

  private static void checkLessThen(String revision1, String revision2) {
    CvsRevisionNumber revNumber1 = new CvsRevisionNumber(revision1);
    CvsRevisionNumber revNumber2 = new CvsRevisionNumber(revision2);
    assertEquals(-1, revNumber1.compareTo(revNumber2));
    assertNotEquals(revNumber1, revNumber2);
  }

  private static void checkRevisionsAreEqual(String revision1, String revision2) {
    CvsRevisionNumber revisionNumber1 = new CvsRevisionNumber(revision1);
    CvsRevisionNumber revisionNumber2 = new CvsRevisionNumber(revision2);
    assertEquals(0, revisionNumber1.compareTo(revisionNumber2));
    assertEquals(revisionNumber1, revisionNumber2);
    assertEquals(revisionNumber1.hashCode(), revisionNumber2.hashCode());
  }

}
