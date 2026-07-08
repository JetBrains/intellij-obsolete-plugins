package com.intellij.lang.puppet.project.structure;

import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.lang.puppet.project.PuppetEntity;
import com.intellij.lang.puppet.project.PuppetProjectModel;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class PuppetTreeStructureProvider implements TreeStructureProvider {
  @Override
  public @NotNull Collection<AbstractTreeNode<?>> modify(@NotNull AbstractTreeNode<?> parent,
                                                         @NotNull Collection<AbstractTreeNode<?>> children,
                                                         ViewSettings settings) {

    return ContainerUtil.map(children, node -> {
      if (!(node instanceof PsiDirectoryNode)) {
        return node;
      }

      Project nodeProject = node.getProject();
      if (nodeProject == null || nodeProject.isDefault()) {
        return node;
      }

      PuppetEntity entity =
        PuppetProjectModel.getInstance(nodeProject).getPuppetModuleOrEnvironment(((PsiDirectoryNode)node).getVirtualFile());
      if (entity != null) {
        return PuppetPsiDirectoryNode.getNode((PsiDirectoryNode)node, entity.getMetadata());
      }

      return node;
    });
  }
}
