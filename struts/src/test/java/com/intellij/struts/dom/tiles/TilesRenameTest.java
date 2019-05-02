package com.intellij.struts.dom.tiles;

import com.intellij.struts.StrutsTest;

/**
 * @author Yann C&eacute;bron
 */
public class TilesRenameTest extends StrutsTest {

  @Override
  protected String getBasePath() {
    return "/tiles/rename";
  }

  public void testDefinitionRename() {
    myFixture.testRename("definition-rename.xml", "definition-rename_after.xml", "newName");
  }
}
