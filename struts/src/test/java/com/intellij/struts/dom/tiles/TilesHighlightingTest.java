package com.intellij.struts.dom.tiles;

import com.intellij.struts.StrutsTest;
import com.intellij.testFramework.TestDataPath;

/**
 * @author Yann C&eacute;bron
 */
@TestDataPath("$CONTENT_ROOT/../testData/tiles/highlighting")
public class TilesHighlightingTest extends StrutsTest {

  @Override
  protected String getBasePath() {
    return  "/tiles/highlighting";
  }

  public void testTiles20() {
    myFixture.testHighlighting("tiles-20.xml");
  }

  public void testTiles21() {
    myFixture.testHighlighting("tiles-21.xml");
  }

  public void testTiles30() {
    myFixture.testHighlighting("tiles-30.xml");
  }
}
