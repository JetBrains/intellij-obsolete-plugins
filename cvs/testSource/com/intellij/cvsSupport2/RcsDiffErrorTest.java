package com.intellij.cvsSupport2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * author: lesya
 */
public class RcsDiffErrorTest extends CvsTestsWorkingWithImportedProject{
  private List myLines = new ArrayList();
  private static final String ANOTHER_LINE_SEPARATOR = "\r";

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    createLongContent();
  }

  public void testAddingFirstLine() throws Exception {
    executeWith(firstLineAppender());
  }

  public void testRemovingFirstLine() throws Exception {

    executeWith(firstLineRemover());
  }

  public void testSwappingLines() throws Exception {

    Runnable changer = () -> {
      ArrayList result = new ArrayList();
      result.add("");
      result.add("");
      result.add("");
      result.add("");
      for (int i = 0; i < myLines.size() / 2 - 6; i++){
        result.add(myLines.get(i*2 + 1));
        result.add(myLines.get(i*2 ));
      }

      myLines = result;
    };

    executeWith(changer);
  }

  private Runnable firstLineRemover() {
    return () -> myLines.remove(0);
  }


  private void executeWith(Runnable contentModificationAction) throws Exception {
    TEST_FILE.changeContentTo(createContentOnLines());
    TEST_FILE.addToVcs(myVcs);
    commitTransaction();

    modifyFileContent(contentModificationAction);

    updateTestFile();

    String expected = createContentOnLines(ANOTHER_LINE_SEPARATOR);
    String actual = TEST_FILE.getContent();
    assertEquals(expected, actual);
  }

  private Runnable firstLineAppender() {
    return () -> {
      myLines.add(0, "");
      myLines.add(0, "change");
      myLines.add(0, "");
      myLines.add(0, "");
    };
  }

  private void modifyFileContent(Runnable contentModificationAction) throws Exception {
    contentModificationAction.run();
    createNewTestFileRevisionWithContent(createContentOnLines());
  }

  private String createContentOnLines() {
    return createContentOnLines(ANOTHER_LINE_SEPARATOR);
  }

  private String createContentOnLines(String lineSeparator) {
    StringBuilder result = new StringBuilder();
    for (Iterator iterator = myLines.iterator(); iterator.hasNext();) {
      result.append((String) iterator.next());
      if (iterator.hasNext())
        result.append(lineSeparator);
    }
    return result.toString();
  }

  private StringBuffer createLongContent() {
    StringBuffer longContent = new StringBuffer();
    for(int i = 0; i < 1000; i++){
      myLines.add("String " + i);
    }
    return longContent;
  }
}
