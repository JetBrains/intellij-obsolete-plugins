/*
 * Copyright (c) 2000-2006 JetBrains s.r.o. All Rights Reserved.
 */

package com.intellij.gwt.jsni;

import com.intellij.gwt.GwtTestCase;
import com.intellij.gwt.jsinject.JsInjector;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.lang.javascript.JavaScriptSupportLoader;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.LightJavaCodeInsightTestCase;
import com.intellij.testFramework.TestDataPath;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

@TestDataPath("$CONTENT_ROOT/../testData")
@SuppressWarnings({"HardCodedStringLiteral"})
public class EditingJsniMethodTest extends LightJavaCodeInsightTestCase {
  @NonNls private static final String COMMENT_START = "/*-{";

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    InjectedLanguageManager.getInstance(getProject()).registerMultiHostInjector(new JsInjector(), getTestRootDisposable());
  }

  @NotNull
  @Override
  protected String getTestDataPath() {
    return GwtTestCase.getGwtTestDataPath();
  }

  public void testRemoveAndRestoreJsniComment() {
    configureByFile("/jsni/injection/JsniMethod.java");
    assertJavaScriptInjected();
    for (int i = 1; i < 7; i++) {
      deleteAndRestore(i);
    }
  }

  public void testEditingMethodName() {
    configureByFile("/jsni/injection/JsniMethod.java");
    final Document document = getEditor().getDocument();
    final String oldText = document.getText();
    final int offset = oldText.indexOf("method()");
    WriteCommandAction.runWriteCommandAction(getProject(), () -> document.insertString(offset, "a"));

    commitDocument(document);
    assertJavaScriptInjected();
    String newText = new StringBuilder(oldText).insert(offset, "a").toString();
    assertEquals(newText, document.getText());
  }

  public void testInsertJsniComment() {
    configureByFile("/jsni/injection/JsniMethodWithoutComment.java");
    final Document document = getEditor().getDocument();
    final int offset = document.getText().indexOf("m()") + 3;
    String text = "/*-{ $wnd.alert(\"a\") }-*/;";
    for (int i = 0; i < text.length(); i++) {
      final int finalI = i;
      WriteCommandAction.runWriteCommandAction(getProject(), () -> document.insertString(offset + finalI, String.valueOf(text.charAt(finalI))));

      commitDocument(document);
      if (i < text.length() - 2) {
        assertJavaScriptNotInjected();
      }
    }
    assertJavaScriptInjected();
  }

  private void deleteAndRestore(int x) {
    final Document document = getEditor().getDocument();
    final int offset = document.getText().indexOf(COMMENT_START) + COMMENT_START.length();
    String deleted = document.getText().substring(offset - x, offset);
    WriteCommandAction.runWriteCommandAction(getProject(), () -> document.deleteString(offset - x, offset));

    commitDocument(document);
    assertJavaScriptNotInjected();
    WriteCommandAction.runWriteCommandAction(getProject(), () -> document.insertString(offset - x, deleted));

    commitDocument(document);
    assertJavaScriptInjected();
  }

  private void assertJavaScriptInjected() {
    assertTrue(isJavaScriptInjected());
  }

  private void assertJavaScriptNotInjected() {
    assertFalse(isJavaScriptInjected());
  }

  private boolean isJavaScriptInjected() {
    final PsiElement element = getFile().findElementAt(getEditor().getDocument().getText().indexOf("alert"));
    if (element == null) return false;

    final Ref<Boolean> result = new Ref<>(Boolean.FALSE);
    InjectedLanguageManager.getInstance(getProject()).enumerate(element, (injectedPsi, places) -> {
      if (injectedPsi.getLanguage().isKindOf(JavaScriptSupportLoader.JAVASCRIPT.getLanguage())) {
        result.set(Boolean.TRUE);
      }
    });

    return result.get().booleanValue();
  }
}
