// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.lang.properties.editor;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.PropertiesImplUtil;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarInfo;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JTree;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * IDEA-393156: the editor must write the value of each locale editor that the user changed, not only the value of the last focused one,
 * and must never write an editor's value into a property other than the one it was edited for.
 */
public class ResourceBundleEditorSaveTest extends BasePlatformTestCase {
  private static final String KEEP_EMPTY_VALUES_KEY = "resource.bundle.editor.insert.empty.values";

  private PropertiesFile myDefaultFile;
  private PropertiesFile myEnFile;
  private ResourceBundleEditor myEditor;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    PsiFile defaultPsi = myFixture.addFileToProject("p.properties", "a=a_default\nb=b_default\n");
    PsiFile enPsi = myFixture.addFileToProject("p_en.properties", "a=a_en\nb=b_en\n");
    myDefaultFile = PropertiesImplUtil.getPropertiesFile(defaultPsi);
    myEnFile = PropertiesImplUtil.getPropertiesFile(enPsi);
    myEditor = new ResourceBundleEditor(getProject(), defaultPsi.getVirtualFile(), myDefaultFile.getResourceBundle());
    Disposer.register(getTestRootDisposable(), myEditor);
    PlatformTestUtil.waitWhileBusy(myEditor.getStructureViewComponent().getTree());
  }

  public void testEditsInSeveralLocalesAreSavedWhenSelectionChanges() {
    selectProperty("a");

    typeInto(myDefaultFile, "a_default_edited");
    typeInto(myEnFile, "a_en_edited");

    selectProperty("b");

    IProperty a = myDefaultFile.findPropertyByKey("a");
    assertNotNull(a);
    assertEquals("a_default_edited", a.getValue());
    IProperty propertyByKey = myEnFile.findPropertyByKey("a");
    assertNotNull(propertyByKey);
    assertEquals("a_en_edited", propertyByKey.getValue());
  }

  public void testEditIsSavedWhenSelectionIsClearedBeforeSelectingAnotherProperty() {
    selectProperty("a");

    typeInto(myDefaultFile, "a_default_edited");

    clearSelection();
    selectProperty("b");
    selectProperty("a");

    IProperty a = myDefaultFile.findPropertyByKey("a");
    assertNotNull(a);
    assertEquals("a_default_edited", a.getValue());
    IProperty b = myDefaultFile.findPropertyByKey("b");
    assertNotNull(b);
    assertEquals("b_default", b.getValue());
  }

  public void testEditsSurviveRecreationOfEditorsPanel() {
    selectProperty("a");

    typeInto(myDefaultFile, "a_default_edited");
    typeInto(myEnFile, "a_en_edited");

    myEditor.recreateEditorsPanel();
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    assertEquals("a_default_edited", myEditor.getTranslationEditors().get(myDefaultFile.getVirtualFile()).getDocument().getText());

    selectProperty("b");

    IProperty a = myDefaultFile.findPropertyByKey("a");
    assertNotNull(a);
    assertEquals("a_default_edited", a.getValue());
    IProperty propertyByKey = myEnFile.findPropertyByKey("a");
    assertNotNull(propertyByKey);
    assertEquals("a_en_edited", propertyByKey.getValue());
  }

  public void testPropagatedValueIsNotRevertedByPendingEdits() {
    selectProperty("a");

    typeInto(myEnFile, "a_en_edited");
    typeInto(myDefaultFile, "a_propagated");

    List<String> infos = collectStatusBarInfo();
    myEditor.propagateValue(getTranslationEditor(myDefaultFile));
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    assertEquals("a_propagated", getTranslationEditor(myEnFile).getDocument().getText());
    assertEmpty(infos);

    selectProperty("b");

    assertPropertyValue(myDefaultFile, "a", "a_propagated");
    assertPropertyValue(myEnFile, "a", "a_propagated");
  }

  public void testExternalChangeIsNotOverwrittenByEditRestoredOnRecreation() {
    selectProperty("a");

    typeInto(myDefaultFile, "a_default_edited");
    typeInto(myEnFile, "a_en_edited");
    setPropertyValueExternally(myEnFile, "a", "a_en_external");

    myEditor.recreateEditorsPanel();
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    assertEquals("a_default_edited", getTranslationEditor(myDefaultFile).getDocument().getText());
    assertEquals("a_en_external", getTranslationEditor(myEnFile).getDocument().getText());

    selectProperty("b");

    assertPropertyValue(myDefaultFile, "a", "a_default_edited");
    assertPropertyValue(myEnFile, "a", "a_en_external");
  }

  public void testEditSurvivesRefreshCausedByChangeInAnotherLocale() {
    selectProperty("a");

    typeInto(myDefaultFile, "a_default_edited");
    setPropertyValueExternally(myEnFile, "a", "a_en_external");

    myEditor.updateEditorsFromProperties(true);
    assertEquals("a_default_edited", getTranslationEditor(myDefaultFile).getDocument().getText());
    assertEquals("a_en_external", getTranslationEditor(myEnFile).getDocument().getText());

    selectProperty("b");

    assertPropertyValue(myDefaultFile, "a", "a_default_edited");
    assertPropertyValue(myEnFile, "a", "a_en_external");
  }

  public void testEditsAreSavedOnDispose() {
    selectProperty("a");

    typeInto(myDefaultFile, "a_default_edited");
    typeInto(myEnFile, "a_en_edited");

    Disposer.dispose(myEditor);

    assertPropertyValue(myDefaultFile, "a", "a_default_edited");
    assertPropertyValue(myEnFile, "a", "a_en_edited");
  }

  public void testEditsAreSavedWhenSwitchingToAnotherFileEditor() {
    selectProperty("a");

    typeInto(myDefaultFile, "a_default_edited");
    typeInto(myEnFile, "a_en_edited");

    myEditor.onSelectionChanged(new FileEditorManagerEvent(FileEditorManager.getInstance(getProject()),
                                                           myDefaultFile.getVirtualFile(), myEditor, null, null, null, null));

    assertPropertyValue(myDefaultFile, "a", "a_default_edited");
    assertPropertyValue(myEnFile, "a", "a_en_edited");
    assertFalse(FileDocumentManager.getInstance().isFileModified(myDefaultFile.getVirtualFile()));
    assertFalse(FileDocumentManager.getInstance().isFileModified(myEnFile.getVirtualFile()));
  }

  public void testEditRestoredOnRecreationCanBeUndone() {
    selectProperty("a");

    typeInto(myEnFile, "a_en_edited");

    myEditor.recreateEditorsPanel();
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    EditorEx editor = getTranslationEditor(myEnFile);
    assertEquals("a_en_edited", editor.getDocument().getText());

    TextEditor textEditor = TextEditorProvider.getInstance().getTextEditor(editor);
    UndoManager undoManager = UndoManager.getInstance(getProject());
    assertTrue(undoManager.isUndoAvailable(textEditor));
    undoManager.undo(textEditor);
    assertEquals("a_en", editor.getDocument().getText());

    selectProperty("b");

    assertPropertyValue(myEnFile, "a", "a_en");
  }

  public void testEditsAreKeptWhenPropagationFails() throws Exception {
    selectProperty("a");

    typeInto(myEnFile, "a_en_edited");
    WriteAction.run(() -> myDefaultFile.getVirtualFile().setWritable(false));
    try {
      myEditor.propagateValue(getTranslationEditor(myEnFile));
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    }
    finally {
      WriteAction.run(() -> myDefaultFile.getVirtualFile().setWritable(true));
    }

    selectProperty("b");

    assertPropertyValue(myDefaultFile, "a", "a_default");
    assertPropertyValue(myEnFile, "a", "a_en_edited");
  }

  public void testEditOfRenamedPropertyIsWrittenToNewKey() {
    selectProperty("a");

    typeInto(myEnFile, "a_en_edited");
    renameProperty("a", "c");

    myEditor.recreateEditorsPanel();
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    selectProperty("c");
    assertEquals("a_en_edited", getTranslationEditor(myEnFile).getDocument().getText());
    selectProperty("b");

    assertNull(myEnFile.findPropertyByKey("a"));
    assertPropertyValue(myEnFile, "c", "a_en_edited");
  }

  public void testEditOfRenamedPropertyMissingInLocaleIsWrittenToNewKey() {
    WriteCommandAction.runWriteCommandAction(getProject(), () -> Objects.requireNonNull(myEnFile.findPropertyByKey("a")).getPsiElement().delete());
    myEditor.recreateEditorsPanel();
    selectProperty("a");
    assertEquals("", getTranslationEditor(myEnFile).getDocument().getText());

    typeInto(myEnFile, "a_en_added");
    renameProperty("a", "c");

    myEditor.recreateEditorsPanel();
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    selectProperty("c");
    assertEquals("a_en_added", getTranslationEditor(myEnFile).getDocument().getText());
    selectProperty("b");

    assertNull(myEnFile.findPropertyByKey("a"));
    assertPropertyValue(myEnFile, "c", "a_en_added");
  }

  public void testNoDiscardNoticeWhenEditMatchesExternalChange() {
    selectProperty("a");

    typeInto(myEnFile, "a_en_external");
    setPropertyValueExternally(myEnFile, "a", "a_en_external");
    List<String> infos = collectStatusBarInfo();

    myEditor.recreateEditorsPanel();
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    myEditor.updateEditorsFromProperties(true);

    assertEmpty(infos);
  }

  public void testNoDiscardNoticeWhenEditWasReverted() {
    selectProperty("a");

    typeInto(myEnFile, "a_en_edited");
    typeInto(myEnFile, "a_en");
    setPropertyValueExternally(myEnFile, "a", "a_en_external");
    List<String> infos = collectStatusBarInfo();

    myEditor.updateEditorsFromProperties(true);

    assertEquals("a_en_external", getTranslationEditor(myEnFile).getDocument().getText());
    assertEmpty(infos);
  }

  public void testDiscardNoticeWhenEditConflictsWithExternalChange() {
    selectProperty("a");

    typeInto(myEnFile, "a_en_edited");
    setPropertyValueExternally(myEnFile, "a", "a_en_external");
    List<String> infos = collectStatusBarInfo();

    myEditor.recreateEditorsPanel();
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

    assertEquals(List.of(ResourceBundleEditorBundle.message("status.bar.edit.discarded.message", "a")), infos);
  }

  public void testEditRestoredDuringAnotherCommandIsUndoneSeparately() {
    selectProperty("a");

    typeInto(myEnFile, "a_en_edited");
    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      IProperty b = myEnFile.findPropertyByKey("b");
      assertNotNull(b);
      b.setValue("b_en_external");
      myEditor.recreateEditorsPanel();
    });
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    EditorEx editor = getTranslationEditor(myEnFile);
    assertEquals("a_en_edited", editor.getDocument().getText());

    UndoManager.getInstance(getProject()).undo(TextEditorProvider.getInstance().getTextEditor(editor));
    assertEquals("a_en", editor.getDocument().getText());
    assertPropertyValue(myEnFile, "b", "b_en_external");
  }

  public void testEditMadeWhileNoPropertyIsSelectedIsSaved() {
    selectProperty("a");
    clearSelection();

    typeInto(myEnFile, "a_en_edited");

    selectProperty("b");

    assertPropertyValue(myEnFile, "a", "a_en_edited");
    assertPropertyValue(myEnFile, "b", "b_en");
  }

  public void testEditSurvivesRecreationOfEditorsPanelDuringUndo() {
    selectProperty("a");

    setPropertyValueExternally(myEnFile, "b", "b_en_external");
    typeInto(myEnFile, "a_en_edited");

    UndoManager undoManager = UndoManager.getInstance(getProject());
    Document enDocument = FileDocumentManager.getInstance().getDocument(myEnFile.getVirtualFile());
    assertNotNull(enDocument);
    // e.g. undoing the creation of a locale file recreates the panel while the undo is in progress
    enDocument.addDocumentListener(new DocumentListener() {
      @Override
      public void documentChanged(@NotNull DocumentEvent event) {
        if (undoManager.isUndoInProgress()) {
          myEditor.recreateEditorsPanel();
        }
      }
    }, getTestRootDisposable());
    myFixture.openFileInEditor(myEnFile.getVirtualFile());
    undoManager.undo(TextEditorProvider.getInstance().getTextEditor(myFixture.getEditor()));
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    assertEquals("a_en_edited", getTranslationEditor(myEnFile).getDocument().getText());

    selectProperty("b");

    assertPropertyValue(myEnFile, "a", "a_en_edited");
    assertPropertyValue(myEnFile, "b", "b_en");
  }

  public void testPropagatedValueReplacesEditsInLocalesThatAlreadyHaveIt() {
    selectProperty("a");

    typeInto(myEnFile, "a_en_edited");
    typeInto(myDefaultFile, "a_en");

    List<String> infos = collectStatusBarInfo();
    myEditor.propagateValue(getTranslationEditor(myDefaultFile));
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    assertEquals("a_en", getTranslationEditor(myEnFile).getDocument().getText());
    assertEmpty(infos);

    selectProperty("b");

    assertPropertyValue(myDefaultFile, "a", "a_en");
    assertPropertyValue(myEnFile, "a", "a_en");
  }

  public void testEditOfDeletedPropertyDoesNotRecreateIt() {
    selectProperty("a");

    typeInto(myEnFile, "a_en_edited");
    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      for (PropertiesFile file : new PropertiesFile[]{myDefaultFile, myEnFile}) {
        Objects.requireNonNull(file.findPropertyByKey("a")).getPsiElement().delete();
      }
    });

    selectProperty("b");

    assertNull(myDefaultFile.findPropertyByKey("a"));
    assertNull(myEnFile.findPropertyByKey("a"));
  }

  public void testEditOfPropertyDeletedInEditedLocaleOnlyIsWritten() {
    selectProperty("a");

    typeInto(myEnFile, "a_en_edited");
    WriteCommandAction.runWriteCommandAction(getProject(), () -> Objects.requireNonNull(myEnFile.findPropertyByKey("a")).getPsiElement().delete());

    selectProperty("b");

    assertPropertyValue(myEnFile, "a", "a_en_edited");
  }

  public void testRevertedEditDoesNotInsertMissingProperty() {
    WriteCommandAction.runWriteCommandAction(getProject(), () -> Objects.requireNonNull(myEnFile.findPropertyByKey("a")).getPsiElement().delete());
    myEditor.recreateEditorsPanel();
    selectProperty("a");
    PropertiesComponent.getInstance().setValue(KEEP_EMPTY_VALUES_KEY, false, true);
    try {
      typeInto(myEnFile, "x");
      typeInto(myEnFile, "");

      selectProperty("b");
    }
    finally {
      PropertiesComponent.getInstance().setValue(KEEP_EMPTY_VALUES_KEY, true, true);
    }

    assertNull(myEnFile.findPropertyByKey("a"));
  }

  public void testRevertedEditDoesNotReformatProperty() {
    Document enDocument = FileDocumentManager.getInstance().getDocument(myEnFile.getVirtualFile());
    assertNotNull(enDocument);
    String text = "a=x\\:y\nb=b_en\n";
    WriteCommandAction.runWriteCommandAction(getProject(), () -> enDocument.setText(text));
    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
    selectProperty("a");
    myEditor.updateEditorsFromProperties(true);
    String shownValue = getTranslationEditor(myEnFile).getDocument().getText();

    typeInto(myEnFile, "x");
    typeInto(myEnFile, shownValue);

    selectProperty("b");

    assertEquals(text, enDocument.getText());
  }

  public void testEditOfEditorNotFilledDuringUndoIsKept() {
    selectProperty("a");

    setPropertyValueExternally(myEnFile, "b", "b_en_external");
    UndoManager undoManager = UndoManager.getInstance(getProject());
    Document enDocument = FileDocumentManager.getInstance().getDocument(myEnFile.getVirtualFile());
    assertNotNull(enDocument);
    // e.g. undoing the creation of a locale file recreates the panel while the undo is in progress
    enDocument.addDocumentListener(new DocumentListener() {
      @Override
      public void documentChanged(@NotNull DocumentEvent event) {
        if (undoManager.isUndoInProgress()) {
          myEditor.recreateEditorsPanel();
        }
      }
    }, getTestRootDisposable());
    myFixture.openFileInEditor(myEnFile.getVirtualFile());
    undoManager.undo(TextEditorProvider.getInstance().getTextEditor(myFixture.getEditor()));
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    // the editors aren't filled during undo
    assertEquals("", getTranslationEditor(myEnFile).getDocument().getText());

    typeInto(myEnFile, "a_en_edited");
    List<String> infos = collectStatusBarInfo();
    myEditor.updateEditorsFromProperties(true);
    assertEquals("a_en_edited", getTranslationEditor(myEnFile).getDocument().getText());
    myEditor.recreateEditorsPanel();
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    assertEquals("a_en_edited", getTranslationEditor(myEnFile).getDocument().getText());
    assertEmpty(infos);

    selectProperty("b");

    assertPropertyValue(myEnFile, "a", "a_en_edited");
    assertPropertyValue(myEnFile, "b", "b_en");
  }

  /**
   * Deliberate policy: when the property was changed elsewhere after the user started editing it, the change made elsewhere wins
   * and the unsaved edit is discarded with a notice. This holds on every save point, as well as on refresh and recreation.
   */
  public void testSavedEditDoesNotOverwriteExternalChange() {
    assertExternalChangeWinsOnSave(() -> selectProperty("b"));
  }

  public void testEditSavedOnDisposeDoesNotOverwriteExternalChange() {
    assertExternalChangeWinsOnSave(() -> Disposer.dispose(myEditor));
  }

  public void testEditSavedWhenSwitchingToAnotherFileEditorDoesNotOverwriteExternalChange() {
    assertExternalChangeWinsOnSave(() -> myEditor.onSelectionChanged(new FileEditorManagerEvent(
      FileEditorManager.getInstance(getProject()), myDefaultFile.getVirtualFile(), myEditor, null, null, null, null)));
  }

  private void assertExternalChangeWinsOnSave(@NotNull Runnable save) {
    selectProperty("a");

    typeInto(myEnFile, "a_en_edited");
    setPropertyValueExternally(myEnFile, "a", "a_en_external");
    List<String> infos = collectStatusBarInfo();

    save.run();

    assertPropertyValue(myEnFile, "a", "a_en_external");
    assertContainsElements(infos, ResourceBundleEditorBundle.message("status.bar.edit.discarded.message", "a"));
  }

  public void testNoticeWhenEditCannotBeSaved() throws Exception {
    selectProperty("a");

    typeInto(myEnFile, "a_en_edited");
    List<String> infos = collectStatusBarInfo();
    WriteAction.run(() -> myEnFile.getVirtualFile().setWritable(false));
    try {
      selectProperty("b");
    }
    finally {
      WriteAction.run(() -> myEnFile.getVirtualFile().setWritable(true));
    }

    assertPropertyValue(myEnFile, "a", "a_en");
    assertContainsElements(infos, ResourceBundleEditorBundle.message("status.bar.edit.not.saved.message", "a",
                                                                     myEnFile.getVirtualFile().getName()));
  }

  public void testEditsRestoredInSeveralLocalesAreUndoneSeparately() {
    selectProperty("a");

    typeInto(myDefaultFile, "a_default_edited");
    typeInto(myEnFile, "a_en_edited");

    myEditor.recreateEditorsPanel();
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    EditorEx enEditor = getTranslationEditor(myEnFile);
    EditorEx defaultEditor = getTranslationEditor(myDefaultFile);
    assertEquals("a_en_edited", enEditor.getDocument().getText());
    assertEquals("a_default_edited", defaultEditor.getDocument().getText());

    UndoManager.getInstance(getProject()).undo(TextEditorProvider.getInstance().getTextEditor(enEditor));
    assertEquals("a_en", enEditor.getDocument().getText());
    assertEquals("a_default_edited", defaultEditor.getDocument().getText());
  }

  public void testEditIsSavedWhenItsPropertyIsNoLongerShownAfterRecreation() {
    selectProperty("a");
    clearSelection();

    typeInto(myEnFile, "a_en_edited");
    List<String> infos = collectStatusBarInfo();

    myEditor.recreateEditorsPanel();
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

    assertPropertyValue(myEnFile, "a", "a_en_edited");
    assertEmpty(infos);
  }

  public void testEditWhosePropertyIsNoLongerShownAfterRecreationIsSavedOnDispose() {
    selectProperty("a");
    clearSelection();

    typeInto(myEnFile, "a_en_edited");

    myEditor.recreateEditorsPanel();
    Disposer.dispose(myEditor);

    assertPropertyValue(myEnFile, "a", "a_en_edited");
  }

  private void renameProperty(@NotNull String oldKey, @NotNull String newKey) {
    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      for (PropertiesFile file : new PropertiesFile[]{myDefaultFile, myEnFile}) {
        IProperty property = file.findPropertyByKey(oldKey);
        if (property != null) property.setName(newKey);
      }
    });
  }

  /**
   * Collects the texts the editor shows in the status bar. The status bar used in tests doesn't remember them.
   */
  private @NotNull List<String> collectStatusBarInfo() {
    List<String> infos = new ArrayList<>();
    getProject().getMessageBus().connect(getTestRootDisposable()).subscribe(StatusBar.Info.INSTANCE.getTOPIC(), new StatusBarInfo() {
      @Override
      public void setInfo(@Nullable String s) {
        infos.add(s);
      }

      @Override
      public void setInfo(@Nullable String s, @Nullable String requestor) {
        infos.add(s);
      }

      @Override
      public String getInfo() {
        return null;
      }
    });
    return infos;
  }

  private static void assertPropertyValue(@NotNull PropertiesFile file, @NotNull String key, @NotNull String expectedValue) {
    IProperty property = file.findPropertyByKey(key);
    assertNotNull(property);
    assertEquals(expectedValue, property.getValue());
  }

  private void setPropertyValueExternally(@NotNull PropertiesFile file, @NotNull String key, @NotNull String value) {
    IProperty property = file.findPropertyByKey(key);
    assertNotNull(property);
    WriteCommandAction.runWriteCommandAction(getProject(), () -> property.setValue(value));
  }

  private @NotNull EditorEx getTranslationEditor(@NotNull PropertiesFile file) {
    EditorEx editor = myEditor.getTranslationEditors().get(file.getVirtualFile());
    assertNotNull(editor);
    return editor;
  }

  private void clearSelection() {
    myEditor.getStructureViewComponent().getTree().clearSelection();
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    assertNull(myEditor.getSelectedProperty());
  }

  /**
   * Selects the row directly, like a mouse click does. {@code StructureViewComponent.select} fires the listener from a promise
   * callback, which is not a write-safe context.
   */
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
        IProperty selectedProperty = myEditor.getSelectedProperty();
        assertNotNull(selectedProperty);
        assertEquals(key, selectedProperty.getName());
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
