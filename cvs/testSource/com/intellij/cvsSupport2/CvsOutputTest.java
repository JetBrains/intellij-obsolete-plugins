package com.intellij.cvsSupport2;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.util.ui.EditorAdapter;

import java.awt.*;

/**
 * author: lesya
 */
public class CvsOutputTest extends HeavyPlatformTestCase {
  private Editor myEditor;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    EditorFactory editorFactory = EditorFactory.getInstance();
    myEditor = editorFactory.createViewer(editorFactory.createDocument(""), myProject);
  }

  @Override
  protected void tearDown() throws Exception {
    EditorFactory.getInstance().releaseEditor(myEditor);
    myEditor = null;
    super.tearDown();
  }

  public void test(){
    EditorSettings editorSettings = myEditor.getSettings();
    editorSettings.setLineMarkerAreaShown(false);
    editorSettings.setIndentGuidesShown(false);
    editorSettings.setLineNumbersShown(false);
    editorSettings.setFoldingOutlineShown(false);

    EditorAdapter editorAdapter = new EditorAdapter(myEditor, myProject, true);

    TextAttributes textAttributes = new TextAttributes(null, null, null, EffectType.LINE_UNDERSCORE, Font.BOLD);
    for (int i = 0; i < 50000; i++){
      editorAdapter.appendString("String " + i, textAttributes);
    }

  }
}
