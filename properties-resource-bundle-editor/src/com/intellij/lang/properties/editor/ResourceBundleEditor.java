// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.lang.properties.editor;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInspection.incomplete.IncompletePropertyUtil;
import com.intellij.ide.FileSelectInContext;
import com.intellij.ide.SelectInContext;
import com.intellij.ide.structureView.newStructureView.StructureViewComponent;
import com.intellij.ide.ui.customization.CustomActionsSchema;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.smartTree.Sorter;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.PropertiesImplUtil;
import com.intellij.lang.properties.PropertiesUtil;
import com.intellij.lang.properties.ResourceBundle;
import com.intellij.lang.properties.codeInspection.IncompletePropertyInspection;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.PropertiesResourceBundleUtil;
import com.intellij.lang.properties.xml.XmlPropertiesFile;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.application.WriteIntentReadAction;

import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.command.impl.UndoManagerImpl;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.command.undo.UndoUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.FocusChangeListener;
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter;
import com.intellij.openapi.editor.impl.ContextMenuPopupHandler;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlFile;
import com.intellij.ui.*;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.Alarm;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.containers.Stack;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.VisibleForTesting;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class ResourceBundleEditor extends UserDataHolderBase implements DocumentsEditor {
  private static final Logger LOG = Logger.getInstance(ResourceBundleEditor.class);
  private static final @NonNls String VALUES               = "values";
  private static final @NonNls String NO_PROPERTY_SELECTED = "noPropertySelected";
  public static final Key<ResourceBundleEditor> RESOURCE_BUNDLE_EDITOR_KEY = Key.create("resourceBundleEditor");

  private final Project myProject;
  private final VirtualFile myFile;

  private final StructureViewComponent      myStructureViewComponent;
  private final Map<VirtualFile, EditorEx> myEditors;
  private final ResourceBundle              myResourceBundle;
  private final ResourceBundlePropertiesUpdateManager myPropertiesInsertDeleteManager;
  private final Map<VirtualFile, JPanel> myTitledPanels;
  private final JComponent                    myNoPropertySelectedPanel = new NoPropertySelectedPanel().getComponent();
  private final DataProviderPanel myDataProviderPanel;
  // user pressed backslash in the corresponding editor.
  // we cannot store it back to properties file right now, so just append the backslash to the editor and wait for the subsequent chars
  private final Alarm               mySelectionChangeAlarm = new Alarm();

  private final JPanel              myValuesPanel;
  private final JPanel              myStructureViewPanel;
  private volatile boolean    myDisposed;
  private final ResourceBundleEditorFileListener myVfsListener;
  private Editor              mySelectedEditor;
  // editors the user typed into, mapped to the property and value they were showing; written back on selection change, editor switch and dispose
  private final Map<Editor, EditBase> myModifiedEditors = new HashMap<>();
  // edits whose property is no longer shown after the editors were recreated, so the user can't see them anymore; written as soon as possible
  private final List<Pair<VirtualFile, PendingEdit>> myDetachedEdits = new ArrayList<>();
  // the property whose values the editors show, can differ from the selected one, e.g. while nothing is selected in the tree
  private @Nullable String    myShownPropertyName;
  private boolean             myUpdatingEditors;
  private String              myPropertyToSelectWhenVisible;
  private final ResourceBundleEditorHighlighter myHighlighter;

  public ResourceBundleEditor(@NotNull Project project, @NotNull VirtualFile file, @NotNull ResourceBundle resourceBundle) {
    myProject = project;
    myFile = file;

    final JPanel splitPanel = new JPanel();
    myValuesPanel = new JPanel();
    myStructureViewPanel = new JPanel();
    JBSplitter splitter = new OnePixelSplitter(false);
    splitter.setFirstComponent(myStructureViewPanel);
    splitter.setSecondComponent(myValuesPanel);
    splitter.setShowDividerControls(true);
    splitter.setHonorComponentsMinimumSize(true);
    splitter.setAndLoadSplitterProportionKey(getClass() + ".splitter");
    splitPanel.setLayout(new BorderLayout());
    splitPanel.add(splitter, BorderLayout.CENTER);

    myResourceBundle = resourceBundle;
    myPropertiesInsertDeleteManager = new ResourceBundlePropertiesUpdateManager(resourceBundle);

    myStructureViewComponent = new ResourceBundleStructureViewComponent(myResourceBundle, this);
    myStructureViewPanel.setLayout(new BorderLayout());
    myStructureViewPanel.add(myStructureViewComponent, BorderLayout.CENTER);

    myStructureViewComponent.getTree().getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
      private IProperty selectedProperty;
      private VirtualFile selectedPropertiesFile;

      @Override
      public void valueChanged(TreeSelectionEvent e) {
        // filter out temp unselect/select events
        if (getSelectedElementIfOnlyOne() instanceof ResourceBundleFileStructureViewElement) {
          ((CardLayout)myValuesPanel.getLayout()).show(myValuesPanel, NO_PROPERTY_SELECTED);
          writeModifiedEditors();
          myShownPropertyName = null;
          selectedPropertiesFile = null;
          selectedProperty = null;
          return;
        }

        if (Comparing.equal(e.getNewLeadSelectionPath(), e.getOldLeadSelectionPath()) || getSelectedProperty() == null) return;
        if (!arePropertiesEquivalent(selectedProperty, getSelectedProperty()) ||
            !Comparing.equal(selectedPropertiesFile, getSelectedPropertiesFile())) {
          writeModifiedEditors();
          selectedProperty = getSelectedProperty();
          selectedPropertiesFile = getSelectedPropertiesFile();
          selectionChanged();
        }
      }

      private static boolean arePropertiesEquivalent(@Nullable IProperty oldSelected, @Nullable IProperty newSelected) {
        if (oldSelected == newSelected) {
          return true;
        }
        if (oldSelected == null || newSelected == null) {
          return false;
        }
        return ReadAction.computeBlocking(() -> {
            final PsiElement oldPsiElement = oldSelected.getPsiElement();
            if (!oldPsiElement.isValid()) {
                return false;
            }
            return oldPsiElement.isEquivalentTo(newSelected.getPsiElement());
        });
      }
    });

    myEditors = new ConcurrentHashMap<>();
    myTitledPanels = new HashMap<>();
    recreateEditorsPanel();

    TreeElement[] children = myStructureViewComponent.getTreeModel().getRoot().getChildren();
    if (children.length != 0) {
      TreeElement child = children[0];
      IProperty property = ((PropertyStructureViewElement)child).getProperty();
      if (property != null) {
        String propName = property.getUnescapedKey();
        setState(new ResourceBundleEditorState(propName));
      }
    }
    myDataProviderPanel = new DataProviderPanel(splitPanel);

    myVfsListener = installPropertiesChangeListeners();

    myProject.getMessageBus().connect(this).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
      @Override
      public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        onSelectionChanged(event);
      }
    });
    myHighlighter = myResourceBundle.getDefaultPropertiesFile() instanceof XmlPropertiesFile ? null : new ResourceBundleEditorHighlighter(this);
  }

  @Override
  public @NotNull VirtualFile getFile() {
    return myFile;
  }

  public ResourceBundle getResourceBundle() {
    return myResourceBundle;
  }

  public void updateTreeRoot() {
    myStructureViewComponent.rebuild();
  }

  public @NotNull ResourceBundlePropertiesUpdateManager getPropertiesInsertDeleteManager() {
    return myPropertiesInsertDeleteManager;
  }

  @VisibleForTesting
  void onSelectionChanged(@NotNull FileEditorManagerEvent event) {
    if (!myResourceBundle.isValid()) return;
    // Ignore events which don't target current editor.
    FileEditor oldEditor = event.getOldEditor();
    FileEditor newEditor = event.getNewEditor();
    if (oldEditor != this && newEditor != this) {
      return;
    }

    // Switching away from this editor — persist any pending edits before losing context.
    if (oldEditor == this) {
      saveModifiedEditors();
    }

    // We want to sync selected property key on selection change.
    if (newEditor == this) {
      if (oldEditor instanceof TextEditor te) {
        myPropertiesInsertDeleteManager.reload();
        setStructureViewSelectionFromPropertiesFile(te.getEditor());
      }
      else if (myPropertyToSelectWhenVisible != null) {
        setStructureViewSelection(myPropertyToSelectWhenVisible);
        myPropertyToSelectWhenVisible = null;
      }
    }
    else if (newEditor instanceof TextEditor te) {
      setPropertiesFileSelectionFromStructureView(te.getEditor());
    }
  }

  private void saveModifiedEditors() {
    if (myModifiedEditors.isEmpty() && myDetachedEdits.isEmpty()) return;
    writeModifiedEditors();
    myVfsListener.flush();
  }

  private void writeModifiedEditors() {
    final Map<VirtualFile, PendingEdit> edits = takeModifiedEditors();
    if (edits.isEmpty() && myDetachedEdits.isEmpty()) return;
    // writing a pending edit reads PSI, may ask to make the file writable and runs a write command,
    // so it needs the write-intent lock rather than a plain read action
    WriteIntentReadAction.run((Runnable)() -> {
      writeDetachedEdits();
      for (Map.Entry<VirtualFile, PendingEdit> entry : edits.entrySet()) {
        writePendingEdit(entry.getKey(), entry.getValue());
      }
    });
  }

  private void writeDetachedEdits() {
    if (myDetachedEdits.isEmpty()) return;
    final List<Pair<VirtualFile, PendingEdit>> edits = new ArrayList<>(myDetachedEdits);
    myDetachedEdits.clear();
    for (Pair<VirtualFile, PendingEdit> edit : edits) {
      writePendingEdit(edit.first, edit.second);
    }
  }

  private void writePendingEdit(@NotNull VirtualFile file, @NotNull PendingEdit edit) {
    // the user reverted the edit, writing it would still reformat the property or insert a missing one
    if (edit.value().equals(edit.base().value())) return;
    String propertyName = edit.base().currentPropertyName();
    if (edit.base().isPropertyRemoved() && !isPropertyInBundle(propertyName)) {
      // the property was deleted, writing the edit would recreate it
      notifyEditDiscarded(propertyName);
      return;
    }
    PropertiesFile propertiesFile = PropertiesImplUtil.getPropertiesFile(file, myProject);
    IProperty property = propertiesFile == null ? null : findPropertyInFileWithoutIndex(propertiesFile, propertyName);
    if (property != null) {
      String currentValue = getPropertyEditorValue(property);
      if (!edit.base().value().equals(currentValue)) {
        // the property was changed elsewhere since the user started editing; deliberately, that change wins, as on a refresh from PSI
        // (see ResourceBundleEditorSaveTest.testSavedEditDoesNotOverwriteExternalChange)
        if (edit.base().isLostWhenReplaced(edit.value(), currentValue)) {
          notifyEditDiscarded(propertyName);
        }
        return;
      }
    }
    if (!writeEditorPropertyValue(propertyName, edit.value(), file)) {
      StatusBar.Info.set(ResourceBundleEditorBundle.message("status.bar.edit.not.saved.message", propertyName, file.getName()), myProject);
    }
  }

  private boolean isPropertyInBundle(@NotNull String propertyName) {
    for (PropertiesFile file : myResourceBundle.getPropertiesFiles()) {
      if (findPropertyInFileWithoutIndex(file, propertyName) != null) return true;
    }
    return false;
  }

  /**
   * Captures the text of all modified editors before anything is written, since writing one value may cause the other editors
   * to be refreshed from PSI.
   */
  private @NotNull Map<VirtualFile, PendingEdit> takeModifiedEditors() {
    if (myModifiedEditors.isEmpty()) return Collections.emptyMap();
    Map<VirtualFile, PendingEdit> result = new HashMap<>();
    for (Map.Entry<VirtualFile, EditorEx> entry : myEditors.entrySet()) {
      EditBase base = myModifiedEditors.get(entry.getValue());
      if (base != null) {
        result.put(entry.getKey(), new PendingEdit(base, entry.getValue().getDocument().getText()));
      }
    }
    myModifiedEditors.clear();
    return result;
  }

  /**
   * The property an editor was showing when the user started typing into it, and the value the property had at that moment.
   * The value is taken from PSI rather than from the editor, since the editor may not have been filled yet, e.g. during an undo.
   * A refresh from PSI keeps the user's text only while the property still has that value, so changes made elsewhere win.
   * The property is tracked by a pointer, so that an edit follows a rename of the property.
   */
  private record EditBase(@NotNull String propertyName, @NotNull String value, @Nullable SmartPsiElementPointer<PsiElement> property) {
    @NotNull String currentPropertyName() {
      final PsiElement element = property == null ? null : property.getElement();
      final String name = element instanceof IProperty p ? p.getName() : null;
      // the property was removed, the edit still belongs to its key
      return name == null ? propertyName : name;
    }

    boolean isPropertyRemoved() {
      return property != null && property.getElement() == null;
    }

    /**
     * Whether replacing the edited text with {@code newValue} loses something the user typed.
     */
    boolean isLostWhenReplaced(@NotNull String editedText, @Nullable String newValue) {
      return !editedText.equals(value) && !editedText.equals(newValue);
    }
  }

  private record PendingEdit(@NotNull EditBase base, @NotNull String value) {}

  private void setStructureViewSelectionFromPropertiesFile(@NotNull Editor propertiesFileEditor) {
    int line = propertiesFileEditor.getCaretModel().getLogicalPosition().line;
    Document document = propertiesFileEditor.getDocument();
    if (line >= document.getLineCount()) {
      return;
    }
    final String propertyName = getPropertyName(document, line);
    if (propertyName == null) {
      return;
    }
    setStructureViewSelection(propertyName);
  }

  private void setStructureViewSelection(final @NotNull String propertyName) {
    if (myStructureViewComponent.isDisposed()) {
      return;
    }
    JTree tree = myStructureViewComponent.getTree();
    Object root = tree.getModel().getRoot();
    if (isLoadingChildrenFor(root)) {
      boolean isEditorVisible = false;
      for (FileEditor editor : FileEditorManager.getInstance(myProject).getSelectedEditors()) {
        if (editor == this) {
          isEditorVisible = true;
          break;
        }
      }
      if (!isEditorVisible) {
        myPropertyToSelectWhenVisible = propertyName;
        return;
      }
      mySelectionChangeAlarm.cancelAllRequests();
      mySelectionChangeAlarm.addRequest(() -> {
        mySelectionChangeAlarm.cancelAllRequests();
        setStructureViewSelection(propertyName);
      }, 500);
      return;
    }

    Stack<TreeElement> toCheck = new Stack<>();
    toCheck.push(myStructureViewComponent.getTreeModel().getRoot());

    while (!toCheck.isEmpty()) {
      TreeElement element = toCheck.pop();
      PsiElement value = element instanceof PropertyStructureViewElement
                     ? ((PropertyStructureViewElement)element).getPsiElement()
                     : null;
      final IProperty property = PropertiesImplUtil.getProperty(value);
      if (property != null && propertyName.equals(property.getUnescapedKey())) {
        myStructureViewComponent.select(property, true);
        selectionChanged();
        return;
      } else {
        for (TreeElement treeElement : element.getChildren()) {
          toCheck.push(treeElement);
        }
      }
    }
  }

  public void flush() {
    myVfsListener.flush();
  }

  private static boolean isLoadingChildrenFor(Object nodeObject) {
    if (!(nodeObject instanceof DefaultMutableTreeNode node)) return false;
    int loadingNodes = 0;
    for (int i = 0; i < Math.min(node.getChildCount(), 2); i++) {
      TreeNode child = node.getChildAt(i);
      if (child instanceof LoadingNode) {
        loadingNodes++;
      }
    }
    return loadingNodes > 0 && loadingNodes == node.getChildCount();
  }

  private static @Nullable String getPropertyName(@NotNull Document document, int line) {
    int startOffset = document.getLineStartOffset(line);
    int endOffset = StringUtil.indexOf(document.getCharsSequence(), '=', startOffset, document.getLineEndOffset(line));
    if (endOffset <= startOffset) {
      return null;
    }
    String propertyName = document.getCharsSequence().subSequence(startOffset, endOffset).toString().trim();
    return propertyName.isEmpty() ? null : propertyName;
  }

  private void setPropertiesFileSelectionFromStructureView(@NotNull Editor propertiesFileEditor) {
    String selectedPropertyName = getSelectedPropertyName();
    if (selectedPropertyName == null) {
      return;
    }

    Document document = propertiesFileEditor.getDocument();
    for (int i = 0; i < document.getLineCount(); i++) {
      String propertyName = getPropertyName(document, i);
      if (selectedPropertyName.equals(propertyName)) {
        propertiesFileEditor.getCaretModel().moveToLogicalPosition(new LogicalPosition(i, 0));
        return;
      }
    }
  }

  /**
   * @return false if the file can't be made writable
   */
  private boolean writeEditorPropertyValue(final @NotNull String propertyName,
                                           final @NotNull String currentValue,
                                           final @NotNull VirtualFile file) {
    if (!FileModificationService.getInstance().prepareVirtualFilesForWrite(myProject, Collections.singleton(file))) {
      return false;
    }
    WriteAction.run(() -> WriteCommandAction.runWriteCommandAction(myProject, ResourceBundleEditorBundle.message(
      "resource.bundle.update.property.value"), null, () -> {
      PropertiesFile propertiesFile = PropertiesImplUtil.getPropertiesFile(file, myProject);
      if (propertiesFile == null) return;

      // Look up the property without the stub index (see findPropertyInFileWithoutIndex).
      // For the dominant case — updating an existing property's value — we can mutate the
      // PSI directly and avoid the platform manager's findPropertyByKey call entirely.
      final IProperty existing = findPropertyInFileWithoutIndex(propertiesFile, propertyName);

      boolean shouldDelete = currentValue.isEmpty() &&
                             ResourceBundleEditorKeepEmptyValueToggleAction.keepEmptyProperties() &&
                             !file.equals(myResourceBundle.getDefaultPropertiesFile().getVirtualFile());

      if (shouldDelete) {
        if (existing != null) {
          // Defer to the manager so its key-ordering state stays consistent across files.
          myPropertiesInsertDeleteManager.deletePropertyIfExist(propertyName, propertiesFile);
        }
      } else if (existing != null) {
        if (!Objects.equals(existing.getValue(), currentValue)) {
          existing.setValue(currentValue);
          CodeStyleManager.getInstance(myProject).reformat(existing.getPsiElement());
        }
      } else {
        // Property doesn't exist in this locale yet — let the manager pick the insertion
        // position (it handles alpha-sorted and ordered modes).
        myPropertiesInsertDeleteManager.insertOrUpdateTranslation(propertyName, currentValue, propertiesFile);
      }
    }));
    return true;
  }

  void recreateEditorsPanel() {
    if (!myProject.isOpen() || myDisposed) return;

    myValuesPanel.removeAll();
    myValuesPanel.setLayout(new CardLayout());

    JPanel valuesPanelComponent = new MyJPanel(new GridBagLayout());
    myValuesPanel.add(new JBScrollPane(valuesPanelComponent){
      @Override
      public void updateUI() {
        super.updateUI();
        getViewport().setBackground(UIUtil.getPanelBackground());
      }
    }, VALUES);
    myValuesPanel.add(myNoPropertySelectedPanel, NO_PROPERTY_SELECTED);

    final List<PropertiesFile> propertiesFiles = myResourceBundle.getPropertiesFiles();

    GridBagConstraints gc = new GridBagConstraints(0, 0, 0, 0, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH,
                                                   JBUI.insets(5), 0, 0);
    // the panel is also recreated from PSI events, where the edits can't be written yet
    final Map<VirtualFile, PendingEdit> pendingEdits = takeModifiedEditors();
    releaseAllEditors();
    myTitledPanels.clear();
    int y = 0;
    Editor previousEditor = null;
    Editor firstEditor = null;
    for (final PropertiesFile propertiesFile : propertiesFiles) {
      final EditorEx editor = createEditor();
      final Editor oldEditor = myEditors.put(propertiesFile.getVirtualFile(), editor);
      if (firstEditor == null) {
        firstEditor = editor;
      }
      if (previousEditor != null) {
        editor.putUserData(ChooseSubsequentPropertyValueEditorAction.PREV_EDITOR_KEY, previousEditor);
        previousEditor.putUserData(ChooseSubsequentPropertyValueEditorAction.NEXT_EDITOR_KEY, editor);
      }
      previousEditor = editor;
      if (oldEditor != null) {
        EditorFactory.getInstance().releaseEditor(oldEditor);
      }

      editor.setViewer(!propertiesFile.getVirtualFile().isWritable());
      editor.getContentComponent().addKeyListener(new KeyAdapter() {
        @Override
        public void keyTyped(KeyEvent e) {
          if (editor.isViewer()) {
            editor.setViewer(ReadonlyStatusHandler.getInstance(myProject)
                               .ensureFilesWritable(Collections.singletonList(propertiesFile.getVirtualFile())).hasReadonlyFiles());
          }
        }
      });

      editor.addFocusListener(new FocusChangeListener() {
        @Override
        public void focusGained(final @NotNull Editor editor) {
          mySelectedEditor = editor;
        }

        // Intentionally no focusLost handler: writing inside focusLost was racing with the
        // tree selection update and could leave the right-hand editors out of sync.
        // Saves happen on tree selection change (writeModifiedEditors),
        // when switching file editors (onSelectionChanged), and on dispose.
      });
      editor.getDocument().addDocumentListener(new DocumentListener() {
        @Override
        public void beforeDocumentChange(@NotNull DocumentEvent event) {
          if (myUpdatingEditors || myModifiedEditors.containsKey(editor)) return;
          if (myShownPropertyName != null) {
            String value = getPropertyEditorValue(findPropertyInFileWithoutIndex(propertiesFile, myShownPropertyName));
            myModifiedEditors.put(editor, new EditBase(myShownPropertyName, value, createPropertyPointer(propertiesFile, myShownPropertyName)));
          }
        }
      });
      gc.gridx = 0;
      gc.gridy = y++;
      gc.gridheight = 1;
      gc.gridwidth = GridBagConstraints.REMAINDER;
      gc.weightx = 1;
      gc.weighty = 1;
      gc.anchor = GridBagConstraints.CENTER;

      String title = propertiesFile.getName() + PropertiesUtil.getPresentableLocale(propertiesFile.getLocale());
      JPanel comp = new JPanel(new BorderLayout()) {
        @Override
        public Dimension getPreferredSize() {
          Insets insets = getBorder().getBorderInsets(this);
          return new Dimension(100,editor.getLineHeight()*4+ insets.top + insets.bottom);
        }
      };
      comp.add(editor.getComponent(), BorderLayout.CENTER);
      comp.setBorder(IdeBorderFactory.createTitledBorder(title, false));
      myTitledPanels.put(propertiesFile.getVirtualFile(), comp);

      valuesPanelComponent.add(comp, gc);
    }
    if (previousEditor != null) {
      previousEditor.putUserData(ChooseSubsequentPropertyValueEditorAction.NEXT_EDITOR_KEY, firstEditor);
      firstEditor.putUserData(ChooseSubsequentPropertyValueEditorAction.PREV_EDITOR_KEY, previousEditor);
    }

    gc.gridx = 0;
    gc.gridy = y;
    gc.gridheight = GridBagConstraints.REMAINDER;
    gc.gridwidth = GridBagConstraints.REMAINDER;
    gc.weightx = 10;
    gc.weighty = 1;

    valuesPanelComponent.add(new JPanel(), gc);
    selectionChanged();
    myValuesPanel.repaint();
    updateEditorsFromProperties(true);
    restorePendingEdits(pendingEdits);
  }

  /**
   * Points to the property in the given locale, or in another locale when it is missing there, since a rename changes all of them.
   */
  private @Nullable SmartPsiElementPointer<PsiElement> createPropertyPointer(@NotNull PropertiesFile file, @NotNull String propertyName) {
    IProperty property = findPropertyInFileWithoutIndex(file, propertyName);
    for (Iterator<PropertiesFile> it = myResourceBundle.getPropertiesFiles().iterator(); property == null && it.hasNext(); ) {
      property = findPropertyInFileWithoutIndex(it.next(), propertyName);
    }
    return property == null ? null : SmartPointerManager.createPointer(property.getPsiElement());
  }

  /**
   * Puts the unsaved edits back into the recreated editors. An edit is dropped when its locale is no longer part of the bundle,
   * or when the property value was changed elsewhere in the meantime. An edit whose property is no longer shown is written
   * later, since the panel is also recreated from PSI events, where it can't be written yet.
   * The value is taken from PSI, since the editors aren't filled while an undo is in progress.
   */
  private void restorePendingEdits(@NotNull Map<VirtualFile, PendingEdit> pendingEdits) {
    final UndoManager undoManager = UndoManager.getInstance(myProject);
    for (Map.Entry<VirtualFile, PendingEdit> entry : pendingEdits.entrySet()) {
      EditorEx editor = myEditors.get(entry.getKey());
      PendingEdit edit = entry.getValue();
      String propertyName = edit.base().currentPropertyName();
      if (editor != null && !propertyName.equals(myShownPropertyName)) {
        myDetachedEdits.add(Pair.create(entry.getKey(), edit));
        continue;
      }
      Document document = editor == null ? null : editor.getDocument();
      PropertiesFile propertiesFile = PropertiesImplUtil.getPropertiesFile(entry.getKey(), myProject);
      String currentValue = propertiesFile == null || editor == null
                            ? null
                            : getPropertyEditorValue(findPropertyInFileWithoutIndex(propertiesFile, propertyName));
      if (document == null || !edit.base().value().equals(currentValue)) {
        if (edit.base().isLostWhenReplaced(edit.value(), currentValue)) {
          notifyEditDiscarded(propertyName);
        }
        continue;
      }
      if (undoManager.isUndoOrRedoInProgress()) {
        // documents can't be changed undoably during undo
        WriteAction.run(() -> updateDocumentFromPropertyValue(edit.value(), document));
      }
      else {
        // undoable, so that the user can still revert the edit after its undo history was lost together with the old editor;
        // no group id, so that the restored edits of different locales aren't merged into a single undo step
        CommandProcessor.getInstance().executeCommand(myProject, () -> WriteAction.run(
          () -> runUpdatingEditors(() -> document.replaceString(0, document.getTextLength(), edit.value()))), "", null);
      }
      myModifiedEditors.put(editor, edit.base());
    }
    if (!myDetachedEdits.isEmpty()) {
      ApplicationManager.getApplication().invokeLater(this::writeDetachedEdits, __ -> myDisposed);
    }
  }

  private void notifyEditDiscarded(@NotNull String propertyName) {
    StatusBar.Info.set(ResourceBundleEditorBundle.message("status.bar.edit.discarded.message", propertyName), myProject);
  }

  public static @NotNull String getPropertyEditorValue(final @Nullable IProperty property) {
    if (property == null) {
      return "";
    }
    else {
      String rawValue = property.getValue();
      return rawValue == null ? "" : PropertiesResourceBundleUtil.fromPropertyValueToValueEditor(rawValue);
    }
  }

  void updateEditorsFromProperties(final boolean checkIsUnderUndoRedoAction) {
    // Phase 1 (snapshot): gather the values to display on the EDT under WriteIntentReadAction.
    // snapshotEditorsState() must run on the EDT because it reads JTree selection state,
    // and it touches PSI which requires read access — WriteIntentReadAction covers both.
    final EditorsSnapshot snapshot = WriteIntentReadAction.compute(this::snapshotEditorsState);

    ((CardLayout)myValuesPanel.getLayout()).show(myValuesPanel, snapshot.propertyName == null ? NO_PROPERTY_SELECTED : VALUES);
    myShownPropertyName = snapshot.propertyName;
    if (snapshot.propertyName == null) return;

    // Phase 2 (write): apply the snapshot to each editor's document.
    final UndoManagerImpl undoManager = (UndoManagerImpl)UndoManager.getInstance(myProject);
    for (final EditorValueSnapshot entry : snapshot.perFile) {
      final EditorEx editor = myEditors.get(entry.file);
      if (editor == null) continue;
      final Document document = editor.getDocument();
      CommandProcessor.getInstance().executeCommand(null, () -> WriteAction.run(() -> {
        if (!checkIsUnderUndoRedoAction || !undoManager.isActive() || !undoManager.isUndoOrRedoInProgress()) {
          EditBase base = myModifiedEditors.get(editor);
          if (base != null && base.currentPropertyName().equals(snapshot.propertyName) && base.value().equals(entry.value)) {
            // the property wasn't changed since the user started editing, keep the unsaved text
            return;
          }
          if (myModifiedEditors.remove(editor) != null && base.isLostWhenReplaced(document.getText(), entry.value)) {
            notifyEditDiscarded(base.currentPropertyName());
          }
          updateDocumentFromPropertyValue(entry.value, document);
        }
      }), "", this);
      JPanel titledPanel = myTitledPanels.get(entry.file);
      ((TitledBorder)titledPanel.getBorder()).setTitleColor(entry.exists ? UIUtil.getLabelTextForeground() : JBColor.RED);
      titledPanel.repaint();
    }
  }

  private @NotNull EditorsSnapshot snapshotEditorsState() {
    String propertyName = getSelectedPropertyName();
    if (propertyName == null) return new EditorsSnapshot(null, Collections.emptyList());
    List<EditorValueSnapshot> perFile = new ArrayList<>();
    for (PropertiesFile propertiesFile : myResourceBundle.getPropertiesFiles()) {
      VirtualFile vf = propertiesFile.getVirtualFile();
      if (!myEditors.containsKey(vf)) continue;
      IProperty property = findPropertyInFileWithoutIndex(propertiesFile, propertyName);
      perFile.add(new EditorValueSnapshot(vf, getPropertyEditorValue(property), property != null));
    }
    return new EditorsSnapshot(propertyName, perFile);
  }

  private static final class EditorsSnapshot {
    final @Nullable String propertyName;
    final @NotNull List<EditorValueSnapshot> perFile;

    EditorsSnapshot(@Nullable String propertyName, @NotNull List<EditorValueSnapshot> perFile) {
      this.propertyName = propertyName;
      this.perFile = perFile;
    }
  }

  private static final class EditorValueSnapshot {
    final @NotNull VirtualFile file;
    final @NotNull String value;
    final boolean exists;

    EditorValueSnapshot(@NotNull VirtualFile file, @NotNull String value, boolean exists) {
      this.file = file;
      this.value = value;
      this.exists = exists;
    }
  }

  /**
   * Lookup a property by key without routing through the stub index.
   * <p>
   * {@link PropertiesFile#findPropertyByKey(String)} delegates to the stub index for files
   * in project content, and {@link com.intellij.util.indexing.FileBasedIndex#ensureUpToDate}
   * is flagged as a slow operation on the EDT in 2026.1+. We only ever look up keys within
   * a single bundle file here, so iterating that file's properties directly is both legal
   * on the EDT and at least as fast as the index for the typical bundle size.
   */
  private static @Nullable IProperty findPropertyInFileWithoutIndex(@NotNull PropertiesFile file, @NotNull String key) {
    for (IProperty property : file.getProperties()) {
      if (key.equals(property.getUnescapedKey())) {
        return property;
      }
    }
    return null;
  }

  private @NotNull ResourceBundleEditorFileListener installPropertiesChangeListeners() {
    final VirtualFileManager virtualFileManager = VirtualFileManager.getInstance();
    ResourceBundleEditorFileListener myVfsListener = new ResourceBundleEditorFileListener(this);

    virtualFileManager.addVirtualFileListener(myVfsListener, this);
    PsiTreeChangeAdapter psiTreeChangeAdapter = new PsiTreeChangeAdapter() {
      @Override
      public void childAdded(@NotNull PsiTreeChangeEvent event) {
        final PsiFile file = event.getFile();
        if (file instanceof XmlFile) {
          final PropertiesFile propertiesFile = PropertiesImplUtil.getPropertiesFile(file);
          if (propertiesFile != null) {
            final ResourceBundle bundle = propertiesFile.getResourceBundle();
            if (bundle.equals(myResourceBundle) && !myEditors.containsKey(propertiesFile.getVirtualFile())) {
              recreateEditorsPanel();
            }
          }
        }
      }

      @Override
      public void childRemoved(@NotNull PsiTreeChangeEvent event) {
        final PsiFile file = event.getFile();
        final PropertiesFile propertiesFile = PropertiesImplUtil.getPropertiesFile(file);
        if (propertiesFile != null) {
          final ResourceBundle bundle = propertiesFile.getResourceBundle();
          if (bundle.equals(myResourceBundle) && myEditors.containsKey(propertiesFile.getVirtualFile())) {
            final IProperty property = PropertiesImplUtil.getProperty(event.getParent());
            if (property != null && Objects.equals(property.getName(), getSelectedPropertyName())) {
              updateEditorsFromProperties(false);
            }
          }
        }
      }
    };
    PsiManager.getInstance(myProject).addPsiTreeChangeListener(psiTreeChangeAdapter, this);
    return myVfsListener;
  }

  private void selectionChanged() {
    UIUtil.invokeLaterIfNeeded(() -> {
      updateEditorsFromProperties(true);
      final StatusBar statusBar = WindowManager.getInstance().getStatusBar(myProject);
      if (statusBar != null) {
        statusBar.setInfo(ResourceBundleEditorBundle.message("status.bar.selection.changed.message", getSelectedPropertyName()));
      }
    });
  }

  private void updateDocumentFromPropertyValue(final String value, final Document document) {
    runUpdatingEditors(() -> UndoUtil.disableUndoIn(document, () -> document.replaceString(0, document.getTextLength(), value)));
  }

  /**
   * Runs a change of the editors that doesn't come from the user, so it doesn't mark them as modified.
   */
  private void runUpdatingEditors(@NotNull Runnable runnable) {
    final boolean wasUpdating = myUpdatingEditors;
    myUpdatingEditors = true;
    try {
      runnable.run();
    }
    finally {
      myUpdatingEditors = wasUpdating;
    }
  }

  private @NotNull JBIterable<Object> getSelectedNodes() {
    if (!isValid()) {
      return JBIterable.empty();
    }
    JTree tree = myStructureViewComponent.getTree();
    return JBIterable.of(tree.getSelectionModel().getSelectionPaths())
      .map(TreeUtil::getLastUserObject);
  }

  private @Nullable String getSelectedPropertyName() {
    final IProperty selectedProperty = getSelectedProperty();
    return selectedProperty == null ? null : ReadAction.computeBlocking(() -> selectedProperty.getName());
  }

  @Nullable
  IProperty getSelectedProperty() {
    ResourceBundleEditorViewElement first = getSelectedNodes()
      .filter(AbstractTreeNode.class)
      .filterMap(AbstractTreeNode::getValue)
      .filter(ResourceBundleEditorViewElement.class)
      .first();
    // resolving the property restores its PSI from a smart pointer, which requires read access
    return first instanceof PropertyStructureViewElement ?
           ReadAction.computeBlocking(() -> ((PropertyStructureViewElement)first).getProperty()) : null;
  }

  public @NotNull @Unmodifiable Collection<ResourceBundleEditorViewElement> getSelectedElements() {
    return getSelectedNodes()
      .filter(AbstractTreeNode.class)
      .filterMap(AbstractTreeNode::getValue)
      .filter(ResourceBundleEditorViewElement.class)
      .toList();
  }

  public @NotNull @Unmodifiable Collection<Object> getSelectedObjects() {
    return getSelectedNodes()
      .filter(AbstractTreeNode.class)
      .filterMap(AbstractTreeNode::getValue)
      .toList();
  }

  public @Nullable Object getSelectedElementIfOnlyOne() {
    final Collection<Object> selectedElements = getSelectedObjects();
    return selectedElements.size() == 1 ? ContainerUtil.getFirstItem(selectedElements) : null;
  }

  public void selectNextIncompleteProperty() {
    if (getSelectedNodes().size() != 1) {
      return;
    }
    final IProperty selectedProperty = getSelectedProperty();
    if (selectedProperty == null) {
      return;
    }

    final ResourceBundleFileStructureViewElement root =
      (ResourceBundleFileStructureViewElement)myStructureViewComponent.getTreeModel().getRoot();
    final Set<String> propertyKeys = ResourceBundleFileStructureViewElement.getPropertiesMap(myResourceBundle, root.isShowOnlyIncomplete()).keySet();
    final boolean isAlphaSorted = myStructureViewComponent.isActionActive(Sorter.getAlphaSorterId());
    List<String> keysOrder = isAlphaSorted ? ContainerUtil.sorted(propertyKeys) : new ArrayList<>(propertyKeys);

    final String currentKey = selectedProperty.getKey();
    final int idx = keysOrder.indexOf(currentKey);
    LOG.assertTrue(idx != -1);
    final IncompletePropertyInspection incompletePropertyInspection =
      IncompletePropertyUtil.getInspection(myResourceBundle.getDefaultPropertiesFile().getContainingFile());
    for (int i = 1; i < keysOrder.size(); i++) {
      int trimmedIndex = (i + idx) % keysOrder.size();
      final String key = keysOrder.get(trimmedIndex);
      if (!IncompletePropertyUtil.isPropertyComplete(incompletePropertyInspection, key, myResourceBundle)) {
        selectProperty(key);
        return;
      }
    }
  }

  @Override
  public @NotNull JComponent getComponent() {
    return myDataProviderPanel;
  }

  public StructureViewComponent getStructureViewComponent() {
    return myStructureViewComponent;
  }

  private VirtualFile getSelectedPropertiesFile() {
    if (mySelectedEditor == null) return null;
    VirtualFile selectedFile = null;
    for (Map.Entry<VirtualFile, EditorEx> entry : myEditors.entrySet()) {
      Editor editor = entry.getValue();
      if (editor == mySelectedEditor) {
        selectedFile = entry.getKey();
        break;
      }
    }
    return selectedFile;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myStructureViewPanel;
  }

  @Override
  public @NotNull String getName() {
    return ResourceBundleEditorBundle.message("resource.bundle.editor.title");
  }

  @Override
  public @NotNull ResourceBundleEditorState getState(@NotNull FileEditorStateLevel level) {
    return new ResourceBundleEditorState(getSelectedPropertyName());
  }

  public void selectProperty(final @Nullable String propertyName) {
    if (propertyName != null) {
      setStructureViewSelection(propertyName);
    }
  }

  @Override
  public void setState(@NotNull FileEditorState state) {
    selectProperty(((ResourceBundleEditorState)state).getPropertyName());
  }

  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public boolean isValid() {
    return !myDisposed && !myProject.isDisposed();
  }

  @Override
  public void deselectNotify() {
    final StatusBar statusBar = WindowManager.getInstance().getStatusBar(myProject);
    if (statusBar != null) {
      statusBar.setInfo("");
    }
  }

  @Override
  public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {

  }

  @Override
  public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {

  }

  @Override
  public BackgroundEditorHighlighter getBackgroundHighlighter() {
    return myHighlighter;
  }

  @Override
  public void dispose() {
    writeModifiedEditors();
    myDisposed = true;
    Disposer.dispose(myStructureViewComponent);
    releaseAllEditors();
  }

  private void releaseAllEditors() {
    myModifiedEditors.clear();
    for (final Editor editor : myEditors.values()) {
      if (!editor.isDisposed()) {
        EditorFactory.getInstance().releaseEditor(editor);
      }
    }
    myEditors.clear();
  }

  @Override
  public @NotNull Document @NotNull [] getDocuments() {
    return ContainerUtil.mapNotNull(myEditors.keySet(), propertiesFile -> FileDocumentManager.getInstance().getDocument(propertiesFile))
      .toArray(Document.EMPTY_ARRAY);
  }

  Map<VirtualFile, EditorEx> getTranslationEditors() {
    return myEditors;
  }

  public static class ResourceBundleEditorState implements FileEditorState {
    private final String myPropertyName;

    public ResourceBundleEditorState(String propertyName) {
      myPropertyName = propertyName;
    }

    @Override
    public boolean canBeMergedWith(@NotNull FileEditorState otherState, @NotNull FileEditorStateLevel level) {
      return false;
    }

    public String getPropertyName() {
      return myPropertyName;
    }
  }

  private EditorEx createEditor() {
    EditorFactory editorFactory = EditorFactory.getInstance();
    Document document = editorFactory.createDocument("");
    EditorEx editor = (EditorEx)editorFactory.createEditor(document);
    reinitSettings(editor);
    editor.putUserData(RESOURCE_BUNDLE_EDITOR_KEY, this);
    return editor;
  }

  private void reinitSettings(final EditorEx editor) {
    EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
    editor.setColorsScheme(scheme);
    editor.setBorder(BorderFactory.createLineBorder(JBColor.border(), 1));
    EditorSettings settings = editor.getSettings();
    settings.setLineNumbersShown(false);
    settings.setWhitespacesShown(false);
    settings.setLineMarkerAreaShown(false);
    settings.setIndentGuidesShown(false);
    settings.setFoldingOutlineShown(false);
    settings.setAdditionalColumnsCount(0);
    settings.setAdditionalLinesCount(0);
    settings.setRightMarginShown(true);
    settings.setRightMargin(60);
    settings.setVirtualSpace(false);
    editor.setHighlighter(new LexerEditorHighlighter(new PropertiesValueHighlighter(), scheme));
    editor.setVerticalScrollbarVisible(true);
    editor.installPopupHandler(new ContextMenuPopupHandler() {
      @Override
      public ActionGroup getActionGroup(@NotNull EditorMouseEvent event) {
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(CustomActionsSchema.getInstance().getCorrectedAction(IdeActions.GROUP_CUT_COPY_PASTE));
        group.add(CustomActionsSchema.getInstance().getCorrectedAction(IdeActions.ACTION_EDIT_SOURCE));
        group.addSeparator();
        group.add(new AnAction(ResourceBundleEditorBundle.messagePointer("action.PropagateValue.text")) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            propagateValue(editor);
          }
        });
        return group;
      }
    });
  }

  @VisibleForTesting
  void propagateValue(@NotNull Editor editor) {
    final String valueToPropagate = editor.getDocument().getText();
    final String currentSelectedProperty = getSelectedPropertyName();
    if (currentSelectedProperty == null) {
      return;
    }
    ApplicationManager.getApplication().runWriteAction(() -> WriteCommandAction.runWriteCommandAction(myProject, ResourceBundleEditorBundle.message("action.PropagateValue.text"), null, () -> {
      try {
        final PropertiesFile[] propertiesFiles = myResourceBundle.getPropertiesFiles().stream().filter(f -> {
          final IProperty property = f.findPropertyByKey(currentSelectedProperty);
          return property == null || !valueToPropagate.equals(property.getValue());
        }).toArray(PropertiesFile[]::new);
        final PsiFile[] filesToPrepare = Arrays.stream(propertiesFiles).map(PropertiesFile::getContainingFile).toArray(PsiFile[]::new);
        if (FileModificationService.getInstance().preparePsiElementsForWrite(filesToPrepare)) {
          for (PropertiesFile file : propertiesFiles) {
            myPropertiesInsertDeleteManager.insertOrUpdateTranslation(currentSelectedProperty, valueToPropagate, file);
          }
          // the propagated value replaces whatever was typed into the other editors; cleared only now, so the edits are kept if propagation fails
          myModifiedEditors.clear();
          recreateEditorsPanel();
        }
      }
      catch (final IncorrectOperationException e1) {
        LOG.error(e1);
      }
    }));
  }

  private final class DataProviderPanel extends JPanel implements UiDataProvider {
    private DataProviderPanel(final JPanel panel) {
      super(new BorderLayout());
      add(panel, BorderLayout.CENTER);
    }

    @Override
    public void uiDataSnapshot(@NotNull DataSink sink) {
      VirtualFile file = getSelectedPropertiesFile();
      sink.set(SelectInContext.DATA_KEY,
               file == null ? null : new FileSelectInContext(myProject, file));
    }
  }

  private final class MyJPanel extends JPanel implements Scrollable{
    private MyJPanel(LayoutManager layout) {
      super(layout);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
      return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
      Editor editor = myEditors.values().iterator().next();
      return editor.getLineHeight()*4;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
      return visibleRect.height;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
      return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
      return false;
    }
  }
}
