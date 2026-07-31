package com.intellij.gwt.run;

import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.icons.GwtIcons;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckboxTreeListener;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChooseGwtModulesDialog extends DialogWrapper {
  private final CheckboxTree myTree;
  private final PackageCheckedTreeNode myRoot;

  public ChooseGwtModulesDialog(@NotNull Module module, @NotNull List<String> selectedGwtModules) {
    super(module.getProject(), true);
    setTitle(GwtBundle.message("dialog.title.choose.gwt.modules.to.load"));
    myRoot = new PackageCheckedTreeNode("");
    Collection<GwtModule> gwtModules = GwtModulesManager.getInstance(module.getProject()).getCompilableGwtModules(module, false);
    Map<String, PackageCheckedTreeNode> packageNodes = new HashMap<>();
    packageNodes.put("", myRoot);
    Set<String> selectedGwtModulesSet = new HashSet<>(selectedGwtModules);
    for (GwtModule gwtModule : gwtModules) {
      String moduleName = gwtModule.getQualifiedName();
      PackageCheckedTreeNode packageNode = getOrCreatePackageNode(StringUtil.getPackageName(moduleName), packageNodes);
      GwtModuleCheckedTreeNode node = new GwtModuleCheckedTreeNode(moduleName);
      node.setChecked(selectedGwtModulesSet.contains(moduleName));
      packageNode.add(node);
    }
    myTree = new CheckboxTree(new GwtModulesCheckboxTreeCellRenderer(), myRoot);
    myTree.addCheckboxTreeListener(new CheckboxTreeListener() {
      @Override
      public void nodeStateChanged(@NotNull CheckedTreeNode node) {
        setOKActionEnabled(!getSelectedModules().isEmpty());
      }
    });
    TreeUtil.expandAll(myTree);
    init();
  }

  @Override
  public @Nullable JComponent getPreferredFocusedComponent() {
    return myTree;
  }

  private static PackageCheckedTreeNode getOrCreatePackageNode(String packageName, Map<String, PackageCheckedTreeNode> nodes) {
    if (!nodes.containsKey(packageName)) {
      CheckedTreeNode parent = getOrCreatePackageNode(StringUtil.getPackageName(packageName), nodes);
      PackageCheckedTreeNode child = new PackageCheckedTreeNode(StringUtil.getShortName(packageName));
      parent.add(child);
      nodes.put(packageName, child);
    }
    return nodes.get(packageName);
  }

  public @NotNull List<String> getSelectedModules() {
    final List<String> modules = new ArrayList<>();
    TreeUtil.traverse(myRoot, node -> {
      if (node instanceof GwtModuleCheckedTreeNode && ((GwtModuleCheckedTreeNode)node).isChecked()) {
        modules.add(((GwtModuleCheckedTreeNode)node).getQualifiedModuleName());
      }
      return true;
    });
    return modules;
  }

  @Override
  protected @Nullable JComponent createCenterPanel() {
    JScrollPane pane = ScrollPaneFactory.createScrollPane(myTree);
    pane.setPreferredSize(JBUI.size(400, 300));
    return pane;
  }

  @Override
  protected @Nullable String getDimensionServiceKey() {
    return "#GWT.ChooseModulesDialog";
  }

  private static class GwtModulesCheckboxTreeCellRenderer extends CheckboxTree.CheckboxTreeCellRenderer {
    @Override
    public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
      ColoredTreeCellRenderer renderer = getTextRenderer();
      if (value instanceof GwtModuleCheckedTreeNode) {
        renderer.append(StringUtil.getShortName(((GwtModuleCheckedTreeNode)value).getQualifiedModuleName()));
        renderer.setIcon(GwtIcons.GoogleSmall);
      }
      else if (value instanceof PackageCheckedTreeNode) {
        renderer.append(((PackageCheckedTreeNode)value).getPackageName());
        renderer.setIcon(AllIcons.Nodes.Folder);
      }
    }
  }

  private static class PackageCheckedTreeNode extends CheckedTreeNode {
    PackageCheckedTreeNode(@NotNull String packageName) {
      super(packageName);
    }

    public @NlsSafe String getPackageName() {
      return (String)getUserObject();
    }
  }

  private static class GwtModuleCheckedTreeNode extends CheckedTreeNode {
    GwtModuleCheckedTreeNode(@NotNull String moduleName) {
      super(moduleName);
    }

    public @NlsSafe String getQualifiedModuleName() {
      return (String)getUserObject();
    }
  }
}
