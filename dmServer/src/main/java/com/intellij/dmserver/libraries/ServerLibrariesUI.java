package com.intellij.dmserver.libraries;

import com.intellij.dmserver.libraries.obr.DownloadBundlesEditor;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.ide.CommonActionsManager;
import com.intellij.ide.TreeExpander;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;

public class ServerLibrariesUI extends JPanel implements Disposable {
  @NonNls
  public static final String PLACE_TOOLBAR = "ServerLibrariesUI#Toolbar";

  private Tree myTree;
  private ServerLibrariesTreeBuilder myBuilder;
  private final TreeExpander myTreeExpander = new TreeExpander() {
    @Override
    public void expandAll() {
      myBuilder.expandAll();
    }

    @Override
    public boolean canExpand() {
      return true;
    }

    @Override
    public void collapseAll() {
      myBuilder.collapseAll();
    }

    @Override
    public boolean canCollapse() {
      return canExpand();
    }
  };

  private final ServerLibrariesContext myContext;

  private JTabbedPane myTabbedPane;
  private JPanel myAddPanel;
  private DownloadBundlesEditor mySearchPanel;

  public ServerLibrariesUI(ServerLibrariesContext context) {
    super();

    myContext = context;

    createContent(this);
  }

  private void createContent(JPanel parent) {
    parent.setLayout(new BorderLayout());

    myTabbedPane = new JBTabbedPane();
    parent.add(myTabbedPane);

    JPanel availablePanel = new JPanel();
    createAvailablePanel(availablePanel);
    myTabbedPane.addTab(DmServerBundle.message("ServerLibrariesUI.tab.title.available"), availablePanel);

    myAddPanel = new JPanel();
    createAddPanel(myAddPanel);
    myTabbedPane.addTab(DmServerBundle.message("ServerLibrariesUI.tab.title.find.install"), myAddPanel);
  }

  private void createAddPanel(JPanel parent) {
    parent.setLayout(new BorderLayout());
    mySearchPanel = new DownloadBundlesEditor(myContext);
    parent.add(mySearchPanel.getMainPanel());
  }

  private void createAvailablePanel(JPanel parent) {
    parent.setLayout(new BorderLayout(0, 2));

    final DefaultTreeModel model = new DefaultTreeModel(new DefaultMutableTreeNode());
    myTree = new Tree(model);
    myTree.setRootVisible(false);
    myTree.setShowsRootHandles(true);
    myBuilder = new ServerLibrariesTreeBuilder(myContext, myTree, model);
    Disposer.register(this, myBuilder);
    TreeUtil.installActions(myTree);

    myTree.expandRow(myTree.getRowCount() - 1);
    myTree.expandRow(myTree.getRowCount() - 1);
    myTree.expandRow(1);

    parent.add(createToolbarPanel(), BorderLayout.WEST);
    parent.add(ScrollPaneFactory.createScrollPane(myTree), BorderLayout.CENTER);
    ToolTipManager.sharedInstance().registerComponent(myTree);

    JPanel buttonsPanel = new JPanel();
    parent.add(buttonsPanel, BorderLayout.EAST);
    buttonsPanel.setLayout(new GridBagLayout());
  }

  private JPanel createToolbarPanel() {
    final DefaultActionGroup group = new DefaultActionGroup();
    AnAction action = CommonActionsManager.getInstance().createExpandAllAction(myTreeExpander, this);
    group.add(action);
    action = CommonActionsManager.getInstance().createCollapseAllAction(myTreeExpander, this);
    group.add(action);
    final ActionToolbar actionToolBar = ActionManager.getInstance().createActionToolbar(
      PLACE_TOOLBAR, group, false);
    final JPanel buttonsPanel = new JPanel(new BorderLayout());
    buttonsPanel.add(actionToolBar.getComponent(), BorderLayout.WEST);
    return buttonsPanel;
  }

  public void initSearch(String packageName) {
    myTabbedPane.setSelectedComponent(myAddPanel);
    mySearchPanel.initSearch(packageName);
  }

  @Override
  public void dispose() {

  }
}
