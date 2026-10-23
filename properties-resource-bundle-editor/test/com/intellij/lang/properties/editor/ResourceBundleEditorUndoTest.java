// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.lang.properties.editor;

import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.PropertiesImplUtil;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.command.impl.UndoManagerImpl;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.actionSystem.TypedAction;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.fileEditor.impl.CurrentEditorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JTree;

/**
 * IDEA-395149: undo in the Resource Bundle Editor must revert the latest value change immediately, also when it isn't saved yet,
 * and successive undos must revert the changes in reverse editing order.
 */
public class ResourceBundleEditorUndoTest extends BasePlatformTestCase {
  private PropertiesFile myDefaultFile;
  private PropertiesFile myEnFile;
  private PropertiesFile myDeFile;
  private ResourceBundleEditor myEditor;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    PsiFile defaultPsi = myFixture.addFileToProject("p.properties", "a=a_default\nb=b_default\n");
    PsiFile enPsi = myFixture.addFileToProject("p_en.properties", "a=a_en\nb=b_en\n");
    PsiFile dePsi = myFixture.addFileToProject("p_de.properties", "a=a_de\nb=b_de\n");
    myDefaultFile = PropertiesImplUtil.getPropertiesFile(defaultPsi);
    myEnFile = PropertiesImplUtil.getPropertiesFile(enPsi);
    myDeFile = PropertiesImplUtil.getPropertiesFile(dePsi);
    myEditor = new ResourceBundleEditor(getProject(), defaultPsi.getVirtualFile(), myDefaultFile.getResourceBundle());
    Disposer.register(getTestRootDisposable(), myEditor);
    PlatformTestUtil.waitWhileBusy(myEditor.getStructureViewComponent().getTree());
    // so that undo doesn't revert the creation of the files
    ((UndoManagerImpl)UndoManager.getInstance(getProject())).dropHistoryInTests();
  }

  public void testUnsavedEditIsUndoneImmediately() {
    selectProperty("a");

    typeInto(myDefaultFile, "a_default_edited");
    typeInto(myEnFile, "a_en_edited");

    undo();
    assertEquals("a_default_edited", getText(myDefaultFile));
    assertEquals("a_en", getText(myEnFile));

    undo();
    assertEquals("a_default", getText(myDefaultFile));
    assertEquals("a_en", getText(myEnFile));
  }

  public void testSavedEditsAreUndoneInReverseEditingOrder() {
    selectProperty("a");

    // the order differs from the order of the files in the bundle, so that it can't be followed by chance
    typeInto(myEnFile, "a_en_edited");
    typeInto(myDefaultFile, "a_default_edited");
    typeInto(myDeFile, "a_de_edited");

    selectProperty("b");
    selectProperty("a");

    undo();
    assertPropertyValue(myDeFile, "a", "a_de");
    assertPropertyValue(myDefaultFile, "a", "a_default_edited");
    assertPropertyValue(myEnFile, "a", "a_en_edited");

    undo();
    assertPropertyValue(myDefaultFile, "a", "a_default");
    assertPropertyValue(myEnFile, "a", "a_en_edited");

    undo();
    assertPropertyValue(myEnFile, "a", "a_en");
  }

  public void testUndoOfSavedEditIsShown() {
    selectProperty("a");

    typeInto(myEnFile, "a_en_edited");

    selectProperty("b");
    selectProperty("a");
    assertEquals("a_en_edited", getText(myEnFile));

    undo();
    assertPropertyValue(myEnFile, "a", "a_en");
    assertEquals("a_en", getText(myEnFile));
  }

  public void testEditsOfAnotherPropertyAreNotUndoneInItsEditors() {
    selectProperty("a");
    typeInto(myEnFile, "a_en_edited");

    selectProperty("b");
    typeInto(myEnFile, "b_en_edited");

    undo();
    assertEquals("b_en", getText(myEnFile));
    assertPropertyValue(myEnFile, "a", "a_en_edited");

    undo();
    assertPropertyValue(myEnFile, "a", "a_en");
    assertEquals("b_en", getText(myEnFile));

    // the typing for property a happened in the same editor, it must not be undone in it
    assertFalse(UndoManager.getInstance(getProject()).isUndoAvailable(myEditor));
  }

  /**
   * Types and undoes like the IDE does: typing records the Resource Bundle Editor as the current editor, and the undo action gets it
   * as the file editor of the data context.
   */
  public void testTypedTextIsUndoneByUndoAction() {
    selectProperty("a");
    UndoManagerImpl undoManager = (UndoManagerImpl)UndoManager.getInstance(getProject());
    undoManager.setOverriddenEditorProvider(new CurrentEditorProvider() {
      @Override
      public FileEditor getCurrentEditor(@Nullable Project project) {
        return myEditor;
      }
    });
    Disposer.register(getTestRootDisposable(), () -> undoManager.setOverriddenEditorProvider(null));

    EditorEx editor = getTranslationEditor(myEnFile);
    editor.getCaretModel().moveToOffset(editor.getDocument().getTextLength());
    // the editor's own context, typing gets the project of its command from it
    for (char c : "_x".toCharArray()) {
      TypedAction.getInstance().actionPerformed(editor, c, editor.getDataContext());
    }
    assertEquals("a_en_x", getText(myEnFile));

    DataContext undoContext = SimpleDataContext.builder()
      .add(CommonDataKeys.PROJECT, getProject())
      .add(PlatformCoreDataKeys.FILE_EDITOR, myEditor)
      .add(CommonDataKeys.EDITOR, editor)
      .add(PlatformCoreDataKeys.CONTEXT_COMPONENT, editor.getContentComponent())
      .build();
    AnAction undoAction = ActionManager.getInstance().getAction(IdeActions.ACTION_UNDO);
    AnActionEvent event = AnActionEvent.createEvent(undoAction, undoContext, null, ActionPlaces.KEYBOARD_SHORTCUT, ActionUiKind.NONE, null);
    ActionUtil.performDumbAwareUpdate(undoAction, event, false);
    assertTrue(event.getPresentation().isEnabled());
    ActionUtil.performAction(undoAction, event);
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

    assertEquals("a_en", getText(myEnFile));
  }

  public void testUndoOfSavedEditKeepsEditedPropertySelected() {
    selectProperty("a");
    UndoManagerImpl undoManager = (UndoManagerImpl)UndoManager.getInstance(getProject());
    // the undo manager records the state of the current editor around each command, and restores it on undo
    undoManager.setOverriddenEditorProvider(new CurrentEditorProvider() {
      @Override
      public FileEditor getCurrentEditor(@Nullable Project project) {
        return myEditor;
      }
    });
    Disposer.register(getTestRootDisposable(), () -> undoManager.setOverriddenEditorProvider(null));

    typeInto(myEnFile, "a_en_edited");

    selectProperty("b");
    selectProperty("a");

    undo();
    assertPropertyValue(myEnFile, "a", "a_en");
    assertEquals("a", myEditor.getState(FileEditorStateLevel.FULL).getPropertyName());
    assertEquals("a_en", getText(myEnFile));
  }

  public void testEditSavedWithAllDocumentsIsUndoneInItsEditor() {
    selectProperty("a");

    typeInto(myEnFile, "a_en_edited");
    FileDocumentManager.getInstance().saveAllDocuments();

    undo();
    assertEquals("a_en", getText(myEnFile));
    assertPropertyValue(myEnFile, "a", "a_en");

    typeInto(myDefaultFile, "a_default_edited");
    undo();
    assertEquals("a_default", getText(myDefaultFile));
    assertEquals("a_en", getText(myEnFile));
  }

  private void undo() {
    UndoManager undoManager = UndoManager.getInstance(getProject());
    assertTrue(undoManager.isUndoAvailable(myEditor));
    undoManager.undo(myEditor);
    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
  }

  private static void assertPropertyValue(@NotNull PropertiesFile file, @NotNull String key, @NotNull String expectedValue) {
    IProperty property = file.findPropertyByKey(key);
    assertNotNull(property);
    assertEquals(expectedValue, property.getValue());
  }

  private @NotNull String getText(@NotNull PropertiesFile file) {
    return getTranslationEditor(file).getDocument().getText();
  }

  private @NotNull EditorEx getTranslationEditor(@NotNull PropertiesFile file) {
    EditorEx editor = myEditor.getTranslationEditors().get(file.getVirtualFile());
    assertNotNull(editor);
    return editor;
  }

  private void selectProperty(@NotNull String key) {
    JTree tree = myEditor.getStructureViewComponent().getTree();
    PlatformTestUtil.expandAll(tree);
    for (int row = 0; row < tree.getRowCount(); row++) {
      AbstractTreeNode<?> node = TreeUtil.getLastUserObject(AbstractTreeNode.class, tree.getPathForRow(row));
      Object element = node == null ? null : node.getValue();
      if (element instanceof PropertyStructureViewElement viewElement && viewElement.getProperty() != null &&
          key.equals(viewElement.getProperty().getKey())) {
        tree.setSelectionRow(row);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        return;
      }
    }
    fail("No tree row for property " + key);
  }

  private void typeInto(@NotNull PropertiesFile file, @NotNull String text) {
    Document document = getTranslationEditor(file).getDocument();
    WriteCommandAction.runWriteCommandAction(getProject(), () -> document.replaceString(0, document.getTextLength(), text));
  }
}
