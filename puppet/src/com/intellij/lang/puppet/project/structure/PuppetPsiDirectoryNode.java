package com.intellij.lang.puppet.project.structure;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.lang.puppet.project.meta.PuppetEnvironmentMetadata;
import com.intellij.lang.puppet.project.meta.PuppetMetadata;
import com.intellij.lang.puppet.project.meta.PuppetModuleMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

abstract class PuppetPsiDirectoryNode<T extends PuppetMetadata> extends PsiDirectoryNode {
  private final T myMetadata;

  PuppetPsiDirectoryNode(PsiDirectoryNode original, T metadata) {
    super(original.getProject(), original.getValue(), original.getSettings());
    myMetadata = metadata;
  }

  @Override
  protected void updateImpl(@NotNull PresentationData data) {
    super.updateImpl(data);
    data.setIcon(computeIcon());
    data.setLocationString("(" + computeLocationString() + ")");
  }

  protected abstract @Nullable Icon computeIcon();

  protected @NotNull String computeLocationString() {
    return myMetadata.getPresentableName();
  }

  public static PuppetPsiDirectoryNode getNode(@NotNull PsiDirectoryNode original, @NotNull PuppetMetadata metadata) {
    if (metadata instanceof PuppetModuleMetadata) {
      return new Module(original, (PuppetModuleMetadata)metadata);
    }
    else if (metadata instanceof PuppetEnvironmentMetadata) {
      return new Environment(original, (PuppetEnvironmentMetadata)metadata);
    }
    throw new RuntimeException("Unknown metadata class: " + metadata.getClass());
  }

  private static class Module extends PuppetPsiDirectoryNode<PuppetModuleMetadata> {
    Module(PsiDirectoryNode original, PuppetModuleMetadata metadata) {
      super(original, metadata);
    }

    @Override
    protected @NotNull Icon computeIcon() {
      return AllIcons.Nodes.Method;
    }
  }

  private static class Environment extends PuppetPsiDirectoryNode<PuppetEnvironmentMetadata> {
    Environment(PsiDirectoryNode original, PuppetEnvironmentMetadata metadata) {
      super(original, metadata);
    }

    @Override
    protected @NotNull Icon computeIcon() {
      return AllIcons.Nodes.Enum;
    }
  }
}
