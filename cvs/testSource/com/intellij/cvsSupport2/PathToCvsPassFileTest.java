package com.intellij.cvsSupport2;

import com.intellij.cvsSupport2.config.CvsApplicationLevelConfiguration;
import junit.framework.TestCase;

public class PathToCvsPassFileTest extends TestCase {
  private static final String USER_HOME_PROPERTY = "user.home";

  public void test() {
    doTest("c:/Documents and Settings", "$userdir/.cvspass", "c:\\Documents and Settings\\.cvspass");
    doTest("c:/Documents and Settings$", "$userdir/.cvspass", "c:\\Documents and Settings$\\.cvspass");
    doTest("c:/Documents and Settings", "userdir/.cvspass", "userdir\\.cvspass");
  }

  private static void doTest(String home_dir, String presentation, String expected) {
    String oldValue = System.setProperty(USER_HOME_PROPERTY, home_dir);
    try {
      String value = CvsApplicationLevelConfiguration.convertToIOFilePath(presentation);
      assertEquals(expected, value);
    }
    finally {
      System.setProperty(USER_HOME_PROPERTY, oldValue);
    }
  }
}
