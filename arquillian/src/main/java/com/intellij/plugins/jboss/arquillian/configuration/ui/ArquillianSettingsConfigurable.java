package com.intellij.plugins.jboss.arquillian.configuration.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MasterDetailsComponent;
import com.intellij.openapi.ui.MasterDetailsStateService;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.ListPopupStep;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.plugins.jboss.arquillian.ArquillianBundle;
import com.intellij.plugins.jboss.arquillian.ArquillianIcons;
import com.intellij.plugins.jboss.arquillian.configuration.ArquillianContainersAppManager;
import com.intellij.plugins.jboss.arquillian.configuration.container.ArquillianContainer;
import com.intellij.plugins.jboss.arquillian.configuration.model.ArquillianContainerModel;
import com.intellij.plugins.jboss.arquillian.configuration.model.ArquillianContainersModel;
import com.intellij.plugins.jboss.arquillian.configuration.model.ArquillianListModel;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianContainersManager;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.IconUtil;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;
import java.util.*;

public final class ArquillianSettingsConfigurable extends MasterDetailsComponent implements SearchableConfigurable {
  private static final String ARQUILLIAN_CONTAINERS_UI_KEY = "ArquillianSettingsConfigurable.UI";
  private final ArquillianContainersManager containersManager;
  private final Project project;
  private final ArquillianContainersModel model;
  private final List<ScopeNode> scopeNodes = new ArrayList<>();
  private final Map<ArquillianContainer.Scope, ScopeNode> scopeToNodesMap = new HashMap<>();
  private final Map<ArquillianContainerModel, ItemNode> modelToItemMap = new HashMap<>();

  public ArquillianSettingsConfigurable(Project project) {
    this.containersManager = ArquillianContainersManager.getInstance(project);
    this.project = project;
    this.model = new ArquillianContainersModel(project, containersManager.getState());
    for (ArquillianContainer.Scope scope : ArquillianContainer.Scope.values()) {
      ScopeNode scopeNode = new ScopeNode(scope);
      scopeToNodesMap.put(scope, scopeNode);
      scopeNodes.add(scopeNode);
    }
    initTree();
    this.model.getListListenersHolder().addListener(new ArquillianListModel.Listener<>() {
      @Override
      public void itemChanged(ArquillianContainerModel model) {
        ItemNode itemNode = modelToItemMap.get(model);
        ((DefaultTreeModel)myTree.getModel()).nodeChanged(itemNode);
      }

      @Override
      public void itemAdded(ArquillianContainerModel item, int index) {
        addItemNode(item);
      }

      @Override
      public void itemRemoved(ArquillianContainerModel item, int index) {
        removeItemNode(item);
      }
    });
  }


  private static DefaultMutableTreeNode getNodeToSelectWhenDeleted(ItemNode itemNode) {
    return itemNode.getNextNode() != null ? itemNode.getNextNode()
                                          : itemNode.getPreviousNode() != null ? itemNode.getPreviousNode()
                                                                               : itemNode.getNextLeaf() != null
                                                                                 ? itemNode.getNextLeaf()
                                                                                 : itemNode.getPreviousLeaf();
  }

  private int getScopeNodeVisibleIndex(ScopeNode scopeNode) {
    int index = 0;
    for (ScopeNode node : scopeNodes) {
      if (node == scopeNode) {
        break;
      }
      if (node.isVisible()) {
        ++index;
      }
    }
    return index;
  }

  private void showScopeNode(ScopeNode scopeNode) {
    if (scopeNode.isVisible()) {
      return;
    }
    int index = getScopeNodeVisibleIndex(scopeNode);
    myRoot.insert(scopeNode, index);
    ((DefaultTreeModel)myTree.getModel()).nodesWereInserted(myRoot, new int[]{index});
    scopeNode.setVisible(true);
  }

  private void hideScopeNode(ScopeNode scopeNode) {
    if (!scopeNode.isVisible()) {
      return;
    }
    int index = getScopeNodeVisibleIndex(scopeNode);
    myRoot.remove(index);
    ((DefaultTreeModel)myTree.getModel()).nodesWereRemoved(myRoot, new int[]{index}, new ScopeNode[]{scopeNode});
    scopeNode.setVisible(false);
  }

  private void addItemNode(ArquillianContainerModel item) {
    ScopeNode scopeNode = scopeToNodesMap.get(item.getDescription().getScope());
    int scopeNodeChildrenCount = scopeNode.getChildCount();
    if (scopeNodeChildrenCount == 0) {
      showScopeNode(scopeNode);
    }
    ItemNode itemNode = new ItemNode(project, item, TREE_UPDATER);
    scopeNode.add(itemNode);
    modelToItemMap.put(item, itemNode);
    ((DefaultTreeModel)myTree.getModel()).nodesWereInserted(scopeNode, new int[]{scopeNode.getChildCount() - 1});
  }

  private void removeItemNode(ArquillianContainerModel item) {
    ScopeNode scopeNode = scopeToNodesMap.get(item.getDescription().getScope());
    ItemNode itemNode = modelToItemMap.remove(item);
    DefaultMutableTreeNode nodeToSelect = getNodeToSelectWhenDeleted(itemNode);
    if (nodeToSelect == null) {
      nodeToSelect = itemNode.getPreviousNode();
    }
    int itemNodeIndex = scopeNode.getIndex(itemNode);
    scopeNode.remove(itemNode);
    if (scopeNode.getChildCount() == 0) {
      hideScopeNode(scopeNode);
    }
    else {
      ((DefaultTreeModel)myTree.getModel()).nodesWereRemoved(scopeNode, new int[]{itemNodeIndex}, new ItemNode[]{itemNode});
    }
    selectNodeInTree(nodeToSelect);
  }

  @Override
  protected void initTree() {
    List<ArquillianContainerModel> modelItems = new ArrayList<>(model.getChildren());
    modelItems.sort(new ArquillianContainerModel.ScopeNameComparator());
    for (ArquillianContainerModel containerModel : modelItems) {
      addItemNode(containerModel);
    }
    super.initTree();
    myTree.setShowsRootHandles(false);
    TreeSpeedSearch.installOn(myTree, true, treePath -> {
      MyNode lastPathComponent = (MyNode)treePath.getLastPathComponent();

      return lastPathComponent.getChildCount() > 0 ? "" : lastPathComponent.getDisplayName();
    });
  }

  @Override
  protected @NotNull ArrayList<AnAction> createActions(boolean fromPopup) {
    final ArrayList<AnAction> result = new ArrayList<>();
    result.add(new AddAction());
    result.add(new RemoveAction());
    result.add(new CloneAction());

    return result;
  }

  @Override
  public boolean isModified() {
    return containersManager.hasChanges(model);
  }

  @Override
  public void apply() throws ConfigurationException {
    checkNoDuplicates();
    containersManager.saveContainersModel(model);
  }

  private void checkNoDuplicates() throws ConfigurationException {
    Set<String> names = new HashSet<>();
    for (ArquillianContainerModel containerModel : model.getChildren()) {
      if (names.contains(containerModel.getName())) {
        throw new ConfigurationException(ArquillianBundle.message("arquillian.container.name.already.exist", containerModel.getName()));
      }
      names.add(containerModel.getName());
    }
  }

  @Override
  protected String getComponentStateKey() {
    return ARQUILLIAN_CONTAINERS_UI_KEY;
  }

  @Override
  protected MasterDetailsStateService getStateService() {
    return MasterDetailsStateService.getInstance(project);
  }

  private void createNewConfiguration(ArquillianContainer container) {
    String name = model.selectUnusedName(container.getName());
    ArquillianContainerModel containerModel = new ArquillianContainerModel(project, name, container);
    model.addItem(containerModel);
    selectNodeInTree(modelToItemMap.get(containerModel));
  }

  private void removeConfiguration(ItemNode itemNode) {
    model.removeItem(itemNode.getModel());
  }

  @NotNull
  @Override
  public String getId() {
    return "arq.settings";
  }

  @Nls
  @Override
  public String getDisplayName() {
    return ArquillianBundle.message("arquillian.containers.configurable.display.name");
  }

  @Override
  public @NotNull String getHelpTopic() {
    return "reference.settings.arquillian.containers";
  }

  private void cloneConfiguration(ItemNode itemNode) {
    ArquillianContainerModel model = this.model.cloneConfiguration(itemNode.getModel());
    selectNodeInTree(modelToItemMap.get(model));
  }

  private void showAddPopup() {
    ArquillianContainersAppManager appManager = ArquillianContainersAppManager.getInstance();
    List<ArquillianContainer.Scope> groups = JBIterable
      .of(ArquillianContainer.Scope.values())
      .filter(scope -> appManager.getContainers(scope).size() > 0)
      .toList();
    ListPopup popup = JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<>(
      ArquillianBundle.message("select.container.configuration"), groups) {

      @Override
      @NotNull
      public String getTextFor(ArquillianContainer.Scope scope) {
        return hasSubstep(scope)
               ? scope.getDescription().get()
               : appManager.getContainers(scope).get(0).getName();
      }

      @Override
      public boolean isSpeedSearchEnabled() {
        return true;
      }

      @Override
      public boolean canBeHidden(ArquillianContainer.Scope scope) {
        return false;
      }

      @Override
      public Icon getIconFor(final ArquillianContainer.Scope scope) {
        return hasSubstep(scope)
               ? ArquillianIcons.Arquillian
               : appManager.getContainers(scope).get(0).getIcon();
      }

      @Override
      public PopupStep onChosen(ArquillianContainer.Scope scope, final boolean finalChoice) {
        if (hasSubstep(scope)) {
          return getSupStep(scope);
        }
        createNewConfiguration(appManager.getContainers(scope).get(0));
        return FINAL_CHOICE;
      }

      private ListPopupStep getSupStep(ArquillianContainer.Scope scope) {
        return new BaseListPopupStep<>(
          scope.getDescription().get(), appManager.getContainers(scope)) {

          @Override
          @NotNull
          public String getTextFor(ArquillianContainer container) {
            return container.getName();
          }

          @Override
          public Icon getIconFor(ArquillianContainer container) {
            return container.getIcon();
          }

          @Override
          public PopupStep onChosen(ArquillianContainer container, final boolean finalChoice) {
            createNewConfiguration(container);
            return FINAL_CHOICE;
          }
        };
      }

      @Override
      public boolean hasSubstep(ArquillianContainer.Scope scope) {
        return appManager.getContainers(scope).size() > 1;
      }
    });
    TreeSpeedSearch.installOn(getTree());

    popup.show(new RelativePoint(myTree, new Point(0, 0)));
  }

  private class RemoveAction extends MyDeleteAction {
    RemoveAction() {
      super(forAll(o -> o instanceof ItemNode));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      TreePath[] paths = myTree.getSelectionPaths();
      if (paths != null) {
        for (TreePath path : paths) {
          Object lastPathComponent = path.getLastPathComponent();
          if (lastPathComponent instanceof ItemNode) {
            removeConfiguration((ItemNode)lastPathComponent);
          }
        }
      }
    }
  }

  private class AddAction extends AnAction implements AnActionButtonRunnable {
    AddAction() {
      super(ArquillianBundle.message("add.new.container.configuration"), null, IconUtil.getAddIcon());
      registerCustomShortcutSet(CommonShortcuts.INSERT, myTree);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      showAddPopup();
    }

    @Override
    public void run(AnActionButton button) {
      showAddPopup();
    }
  }

  private class CloneAction extends AnAction implements ActionUpdateThreadAware, AnActionButtonRunnable {
    CloneAction() {
      super(ArquillianBundle.message("clone.container.configuration"), null, AllIcons.Actions.Copy);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      final Presentation presentation = e.getPresentation();
      presentation.setEnabled(isEnabled());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
      return ActionUpdateThread.EDT;
    }

    private boolean isEnabled() {
      TreePath[] selectionPath = myTree.getSelectionPaths();
      if (selectionPath == null || selectionPath.length != 1) {
        return false;
      }
      return selectionPath[0].getLastPathComponent() instanceof ItemNode;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      cloneSelectedConfiguration();
    }

    @Override
    public void run(AnActionButton button) {
      cloneSelectedConfiguration();
    }

    private void cloneSelectedConfiguration() {
      final TreePath[] selectionPath = myTree.getSelectionPaths();
      if (selectionPath == null || selectionPath.length != 1 || !(selectionPath[0].getLastPathComponent() instanceof ItemNode)) {
        return;
      }
      cloneConfiguration((ItemNode)selectionPath[0].getLastPathComponent());
    }
  }
}
