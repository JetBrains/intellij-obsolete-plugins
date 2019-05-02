package com.intellij.struts.dom.stubs;

import com.intellij.struts.StrutsTest;
import com.intellij.struts.dom.DomStubTest;

import java.io.File;

/**
 * @author Yann C&eacute;bron
 */
@SuppressWarnings("SpellCheckingInspection")
public class TilesStubsTest extends DomStubTest {

  public void testTiles1_1() {
    doBuilderTest("tiles1_1.xml",
                  "File:tiles-definitions\n" +
                  "  Element:tiles-definitions\n" +
                  "    Element:tiles.domnamespace:definition\n" +
                  "      Attribute:name:parent\n" +
                  "      Element:put\n" +
                  "        Attribute:name:header\n" +
                  "        Attribute:value:parentValue\n" +
                  "    Element:tiles.domnamespace:definition\n" +
                  "      Attribute:name:child\n" +
                  "      Attribute:extends:parent\n" +
                  "      Element:put\n" +
                  "        Attribute:name:header\n" +
                  "        Attribute:value:childValue\n");
  }

  public void testTiles3_0() {
    doBuilderTest("tiles3_0.xml",
                  "File:tiles-definitions\n" +
                  "  Element:tiles-definitions\n" +
                  "    Element:tiles.domnamespace:definition\n" +
                  "      Attribute:name:parent\n" +
                  "      Element:put-attribute\n" +
                  "        Attribute:name:header\n" +
                  "        Attribute:value:parentValue\n" +
                  "      Element:put-attribute\n" +
                  "        Attribute:name:\n" +
                  "        Element:definition\n" +
                  "          Attribute:name:nestedDefinition\n");
  }

  @Override
  protected String getTestDataPath() {
    return new File(StrutsTest.getTestDataRoot(), "stubs/tiles").getPath();
  }
}
