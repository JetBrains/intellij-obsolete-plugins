package com.intellij.gwt.sdk;

import com.intellij.pom.java.LanguageLevel;
import junit.framework.TestCase;

import static com.intellij.gwt.sdk.impl.GwtVersionImpl.VERSION_1_6;
import static com.intellij.gwt.sdk.impl.GwtVersionImpl.VERSION_2_0;
import static com.intellij.gwt.sdk.impl.GwtVersionImpl.VERSION_2_4;
import static com.intellij.gwt.sdk.impl.GwtVersionImpl.VERSION_2_5;
import static com.intellij.gwt.sdk.impl.GwtVersionImpl.VERSION_2_6;
import static com.intellij.gwt.sdk.impl.GwtVersionImpl.VERSION_2_7;
import static com.intellij.gwt.sdk.impl.GwtVersionImpl.VERSION_2_8;

public class GwtVersionTest extends TestCase {

  public void testModuleDocType() {
    String docType28 = VERSION_2_8.getGwtModuleDocTypeString();
    assertEquals("<!DOCTYPE module PUBLIC \"-//Google Inc.//DTD Google Web Toolkit 2.8.0//EN\" " +
                 "\"http://gwtproject.org/doctype/2.8.0/gwt-module.dtd\">", docType28);

    String docType27 = VERSION_2_7.getGwtModuleDocTypeString();
    assertEquals("<!DOCTYPE module PUBLIC \"-//Google Inc.//DTD Google Web Toolkit 2.7.0//EN\" " +
                 "\"http://gwtproject.org/doctype/2.7.0/gwt-module.dtd\">", docType27);

    String docType26 = VERSION_2_6.getGwtModuleDocTypeString();
    assertEquals("<!DOCTYPE module PUBLIC \"-//Google Inc.//DTD Google Web Toolkit 2.6.0//EN\" " +
                 "\"http://gwtproject.org/doctype/2.6.0/gwt-module.dtd\">", docType26);

    String docType20 = VERSION_2_0.getGwtModuleDocTypeString();
    assertEquals("<!DOCTYPE module PUBLIC \"-//Google Inc.//DTD Google Web Toolkit 2.0//EN\" " +
                 "\"http://google-web-toolkit.googlecode.com/svn/releases/2.0/distro-source/core/src/gwt-module.dtd\">", docType20);

    String docType16 = VERSION_1_6.getGwtModuleDocTypeString();
    assertEquals("<!DOCTYPE module PUBLIC \"-//Google Inc.//DTD Google Web Toolkit 1.6//EN\" " +
                 "\"http://google-web-toolkit.googlecode.com/svn/releases/1.6/distro-source/core/src/gwt-module.dtd\">", docType16);
  }

  public void testHighestSupportedLanguageLevel() {
    assertEquals(LanguageLevel.JDK_1_8, VERSION_2_8.getHighestSupportedLanguageLevel());
    assertEquals(LanguageLevel.JDK_1_7, VERSION_2_7.getHighestSupportedLanguageLevel());
    assertEquals(LanguageLevel.JDK_1_7, VERSION_2_6.getHighestSupportedLanguageLevel());
    assertEquals(LanguageLevel.JDK_1_6, VERSION_2_5.getHighestSupportedLanguageLevel());
    assertEquals(LanguageLevel.JDK_1_6, VERSION_2_4.getHighestSupportedLanguageLevel());
  }
}
