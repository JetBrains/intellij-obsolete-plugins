package com.intellij.cvsSupport2;

import junit.framework.TestCase;
import org.netbeans.lib.cvsclient.util.SimpleStringPattern;

/**
 * author: lesya
 */
public class SimpleStringPatternTest extends TestCase{
  public void test(){
    SimpleStringPattern pattern = new SimpleStringPattern(".#*");
    assertTrue(pattern.doesMatch(".#file_name1.2.4"));
  }
}
