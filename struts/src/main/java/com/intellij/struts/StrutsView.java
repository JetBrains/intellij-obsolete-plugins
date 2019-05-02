/*
 * Copyright 2000-2006 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.struts;

import com.intellij.ide.actions.ContextHelpAction;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.struts.config.StrutsConfiguration;
import com.intellij.struts.propertyInspector.DomPropertyInspector;
import com.intellij.struts.tree.DomBrowser;
import com.intellij.struts.tree.StrutsDomTree;
import com.intellij.struts.tree.TilesDomTree;
import com.intellij.struts.tree.ValidatorDomTree;
import com.intellij.struts.ui.SmartTabbedPane;
import com.intellij.ui.AutoScrollFromSourceHandler;
import com.intellij.ui.AutoScrollToSourceHandler;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.ui.treeStructure.actions.CollapseAllAction;
import com.intellij.ui.treeStructure.actions.ExpandAllAction;
import com.intellij.util.Alarm;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.tree.AbstractDomElementNode;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;

public class StrutsView implements Disposable {

  private final static int STRUTS_TAB = 0;
  private final static int TILES_TAB = 1;
  private final static int VALIDATION_TAB = 2;

  private final Splitter mySplitter;
  private final SmartTabbedPane myTabbedPane;

  @NotNull
  private final DomBrowser myStrutsBrowser;
  @NotNull
  private final DomBrowser myTilesBrowser;
  @NotNull
  private final DomBrowser myValidatorBrowser;

  private final MyAutoScrollFromSourceHandler myAutoScrollFromSourceHandler;

  public StrutsView(final Project project) {

    myStrutsBrowser = new DomBrowser(new StrutsDomTree(project));
    myTilesBrowser = new DomBrowser(new TilesDomTree(project));
    myValidatorBrowser = new DomBrowser(new ValidatorDomTree(project));

    Disposer.register(this, myStrutsBrowser);
    Disposer.register(this, myTilesBrowser);
    Disposer.register(this, myValidatorBrowser);

    myTabbedPane = new SmartTabbedPane();
    myTabbedPane.addTab("Struts", null, myStrutsBrowser.getComponent(), STRUTS_TAB);
    myTabbedPane.addTab("Tiles", null, myTilesBrowser.getComponent(), TILES_TAB);
    myTabbedPane.addTab("Validator", null, myValidatorBrowser.getComponent(), VALIDATION_TAB);

    mySplitter = new MySplitter();
    mySplitter.setDividerWidth(3);

    final MyAutoScrollToSourceHandler scrollToSourceHandler = new MyAutoScrollToSourceHandler();

    scrollToSourceHandler.install(myStrutsBrowser.getTree());
    scrollToSourceHandler.install(myTilesBrowser.getTree());
    scrollToSourceHandler.install(myValidatorBrowser.getTree());

    myAutoScrollFromSourceHandler = new MyAutoScrollFromSourceHandler(project, this);
    myAutoScrollFromSourceHandler.install();

    final DefaultActionGroup actionGroup = new DefaultActionGroup();
    actionGroup.add(scrollToSourceHandler.createToggleAction());
    actionGroup.add(myAutoScrollFromSourceHandler.createToggleAction());
    actionGroup.addSeparator();

    actionGroup.add(new ExpandAllAction(null) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        final DomBrowser currentBrowser = getCurrentBrowser();
        if (currentBrowser != null) {
          myTree = currentBrowser.getTree();
          super.actionPerformed(e);
        }
      }
    });
    actionGroup.add(new CollapseAllAction(null) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        final DomBrowser currentBrowser = getCurrentBrowser();
        if (currentBrowser != null) {
          myTree = currentBrowser.getTree();
          super.actionPerformed(e);
        }
      }
    });
    actionGroup.addSeparator();
    actionGroup.add(new ContextHelpAction());

    final ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("StrutsView", actionGroup, true);

    JPanel upper = new JPanel(new BorderLayout());
    upper.add(myTabbedPane, BorderLayout.CENTER);
    upper.add(actionToolbar.getComponent(), BorderLayout.NORTH);

    mySplitter.setFirstComponent(upper);

    final DomPropertyInspector table = new DomPropertyInspector();
    Disposer.register(this, table);

    mySplitter.setSecondComponent(ScrollPaneFactory.createScrollPane(table));

    TreeListener.install(table, myStrutsBrowser.getTree());
    TreeListener.install(table, myTilesBrowser.getTree());
    TreeListener.install(table, myValidatorBrowser.getTree());


    myTabbedPane.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent changeEvent) {
        DomElement current = null;
        final DomBrowser browser = getCurrentBrowser();
        if (browser != null) {
          final SimpleNode simpleNode = browser.getTree().getSelectedNode();
          if (simpleNode instanceof AbstractDomElementNode) {
            current = ((AbstractDomElementNode)simpleNode).getDomElement();
          }
        }
        table.setElement(current);
        update();
        scrollToSourceHandler.scrollToSource();
      }
    });
  }

  @Nullable
  protected DomBrowser getCurrentBrowser() {
    switch (myTabbedPane.getVirtualIndex()) {
      case STRUTS_TAB:
        return myStrutsBrowser;
      case TILES_TAB:
        return myTilesBrowser;
      case VALIDATION_TAB:
        return myValidatorBrowser;
      default:
        return null;
    }
  }

  public void update() {
    final DomBrowser browser = getCurrentBrowser();
    if (browser != null) {
      browser.update();
    }
  }

  @NotNull
  public JComponent getComponent() {
    return mySplitter;
  }

  public void openDefault() {

    myStrutsBrowser.openDefault();

    if (myTilesBrowser != null) {
      myTilesBrowser.openDefault();
    }
    if (myValidatorBrowser != null) {
      myValidatorBrowser.openDefault();
    }
  }

  @Override
  public void dispose() {
  }


  protected class MyAutoScrollFromSourceHandler extends AutoScrollFromSourceHandler {

    private final Alarm myAutoscrollAlarm = new Alarm();
    private TextEditor myEditor;
    private CaretListener myListener;

    public MyAutoScrollFromSourceHandler(@NotNull Project project, @Nullable Disposable parentDisposable) {
      super(project, getComponent(), parentDisposable);
    }

    @Override
    protected void selectElementFromEditor(@NotNull FileEditor editor) {
      removeCaretListener();

      if (editor instanceof TextEditor) {
        myEditor = (TextEditor)editor;
        myListener = new CaretListener() {
          @Override
          public void caretPositionChanged(@NotNull CaretEvent e) {
            if (getComponent().isShowing() && isAutoScrollEnabled()) {
              scrollFromSource(e.getEditor());
            }
          }
        };
        scrollFromSource(myEditor.getEditor());
        myEditor.getEditor().getCaretModel().addCaretListener(myListener);
      }

    }

    private void removeCaretListener() {
      if (myEditor != null) {
        myEditor.getEditor().getCaretModel().removeCaretListener(myListener);
        myEditor = null;
        myListener = null;
      }
    }

    protected void scrollToSource() {
    }

    protected void scrollFromSource(final Editor e) {
      if (!getComponent().isShowing() || !isAutoScrollEnabled()) {
        return;
      }

      myAutoscrollAlarm.cancelAllRequests();
      myAutoscrollAlarm.addRequest(() -> {
        if (!e.getContentComponent().hasFocus()) {
          return;
        }
        final PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(e.getDocument());

        // prevent scrolling to source at this point...
        final boolean autoscrollToSource = StrutsConfiguration.getInstance().autoscrollToSource;
        StrutsConfiguration.getInstance().autoscrollToSource = false;
        final DomBrowser currentBrowser = selectFile(psiFile);
        StrutsConfiguration.getInstance().autoscrollToSource = autoscrollToSource;

        if (currentBrowser == null) {
          return;
        }
        final int offset = e.getCaretModel().getOffset();
        assert psiFile != null;
        final XmlTag tag = PsiTreeUtil.findElementOfClassAtOffset(psiFile, offset, XmlTag.class, false);
        if (tag != null) {
          final DomElement domElement = DomManager.getDomManager(myProject).getDomElement(tag);
          if (domElement != null) {
            currentBrowser.setSelectedDomElement(domElement);
          }
        }
      }, 500);
    }

    @Nullable
    protected DomBrowser selectFile(PsiFile file) {
      if (myStrutsBrowser.hasFile(file)) {
        myTabbedPane.setVirtualIndex(STRUTS_TAB);
        return myStrutsBrowser;
      }
      if (myTilesBrowser.hasFile(file)) {
        myTabbedPane.setVirtualIndex(TILES_TAB);
        return myTilesBrowser;
      }
      if (myValidatorBrowser.hasFile(file)) {
        myTabbedPane.setVirtualIndex(VALIDATION_TAB);
        return myValidatorBrowser;
      }
      return null;
    }

    @Override
    protected boolean isAutoScrollEnabled() {
      return StrutsConfiguration.getInstance().autoscrollFromSource;
    }

    @Override
    protected void setAutoScrollEnabled(boolean state) {
      StrutsConfiguration.getInstance().autoscrollFromSource = state;
      if (state && myEditor != null) {
        scrollFromSource(myEditor.getEditor());
      }
    }

    @Override
    public void dispose() {
      super.dispose();
      removeCaretListener();
    }
  }

  public class MyAutoScrollToSourceHandler extends AutoScrollToSourceHandler {

    @Override
    protected boolean isAutoScrollMode() {
      return StrutsConfiguration.getInstance().autoscrollToSource;
    }

    @Override
    protected void setAutoScrollMode(boolean state) {
      StrutsConfiguration.getInstance().autoscrollToSource = state;
      if (state) {
        scrollToSource();
      }
    }

    public void scrollToSource() {
      if (isAutoScrollMode()) {
        final DomBrowser browser = getCurrentBrowser();
        if (browser != null) {
          scrollToSource(browser.getTree());
        }
      }
    }

  }

  private static class TreeListener implements TreeSelectionListener, TreeModelListener {

    public static void install(final DomPropertyInspector table, SimpleTree tree) {
      final TreeListener listener = new TreeListener(table, tree);
      tree.getSelectionModel().addTreeSelectionListener(listener);
      tree.getModel().addTreeModelListener(listener);
    }

    private final DomPropertyInspector myTable;
    private final SimpleTree myTree;

    TreeListener(final DomPropertyInspector table, SimpleTree tree) {
      myTable = table;
      myTree = tree;
    }

    @Override
    public void treeNodesChanged(TreeModelEvent e) {
      setElement();
    }

    @Override
    public void treeNodesInserted(TreeModelEvent e) {
      setElement();
    }

    @Override
    public void treeNodesRemoved(TreeModelEvent e) {
      setElement();
    }

    @Override
    public void treeStructureChanged(final TreeModelEvent e) {
      setElement();
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
      setElement();
    }

    private void setElement() {
      DomElement element = null;
      SimpleNode node = myTree.getSelectedNode();
      if (node instanceof AbstractDomElementNode) {
        element = ((AbstractDomElementNode)node).getDomElement();
        if (element != null && !element.isValid()) {
          element = null;
        }
      }
      myTable.setElement(element);

    }
  }

  private class MySplitter extends Splitter implements DataProvider{
    MySplitter() {
      super(true);
    }

    @Override
    @NonNls
    public Object getData(@NotNull @NonNls final String dataId) {
      if (PlatformDataKeys.HELP_ID.getName().equals(dataId)) {
        switch (myTabbedPane.getVirtualIndex()) {
          case STRUTS_TAB:
            return "reference.tool.windows.struts.assistant.tab.struts";
          case TILES_TAB:
            return "reference.tool.windows.struts.assistant.tab.tiles";
          case VALIDATION_TAB:
            return "reference.tool.windows.struts.assistant.tab.validator";
          default:
            return null;
        }
      }
      return null;
    }
  }

}
