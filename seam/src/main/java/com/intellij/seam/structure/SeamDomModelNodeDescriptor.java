package com.intellij.seam.structure;

import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.javaee.module.view.nodes.JavaeeNodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.seam.model.xml.SeamDomModelManager;
import com.intellij.seam.model.xml.components.SeamComponents;
import com.intellij.seam.model.xml.components.SeamDomComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.xml.DomUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class SeamDomModelNodeDescriptor extends JavaeeNodeDescriptor<XmlFile> {
  @Nullable private final VirtualFile myParentContent;


  public SeamDomModelNodeDescriptor(final Project project,
                                    final NodeDescriptor parentDescriptor,
                                    final Object parameters,
                                    final XmlFile element,
                                    @Nullable VirtualFile parentContent) {
    super(project, parentDescriptor, parameters, element);

    myParentContent = parentContent;
  }

  @Override
  protected String getNewNodeText() {
    VirtualFile file = getJamElement().getVirtualFile();

    return file == null ? "" : file.getPresentableName();
  }

  @Override
  protected Icon getNewIcon() {
    return getJamElement().getIcon(0);
  }

  @Override
  protected void doUpdate() {
    super.doUpdate();
    final String textExt = getNewNodeTextExt();
    if (textExt != null) {
      addColoredFragment(" (" + getNewNodeTextExt() + ")", SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
    }
  }

  @Nullable
  protected String getNewNodeTextExt() {
    XmlFile xmlFile = getJamElement();
    if (xmlFile == null || !xmlFile.isValid() || myParentContent == null) return null;

    return VfsUtilCore.getRelativePath(xmlFile.getVirtualFile(), myParentContent, '/');
  }

  @Override
  public JavaeeNodeDescriptor @NotNull [] getChildren() {
    List<JavaeeNodeDescriptor> children = new ArrayList<>();
    @Nullable SeamComponents model = SeamDomModelManager.getInstance(getProject()).getSeamModel(getJamElement());
    if (model != null) {
      for (SeamDomComponent domComponent : DomUtil.getDefinedChildrenOfType(model, SeamDomComponent.class)) {
          children.add(new SeamDomComponentNodeDescriptor(getProject(), this, getParameters(), domComponent));
      }
    }

    return children.toArray(JavaeeNodeDescriptor.EMPTY_ARRAY);
  }
}
