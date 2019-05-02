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

package com.intellij.struts.tree;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiElement;
import com.intellij.struts.NamedDomModel;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTreeStructure;
import com.intellij.util.JavaeeIcons;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.model.impl.DomModelFactory;
import com.intellij.util.xml.tree.BaseDomElementNode;
import com.intellij.util.xml.tree.DomFileElementNode;
import icons.StrutsApiIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Dmitry Avdeev
 */
public class MultiDomTreeStructure<T extends DomElement, M extends NamedDomModel<T>> extends SimpleTreeStructure {

  private final Project myProject;
  private final DomModelFactory<T, M, PsiElement> myFactory;
  private final Map<Class, Boolean> myHiders;
  private final List<Class> myConsolidated;
  private final List<Class> myFolders;

  public MultiDomTreeStructure(final Project project,
                               final DomModelFactory<T, M, PsiElement> factory,
                               final Map<Class, Boolean> hiders,
                               final List<Class> consolidated,
                               final List<Class> folders) {
    myProject = project;
    myFactory = factory;
    myHiders = hiders;
    myConsolidated = consolidated;
    myFolders = folders;
  }

  @NotNull
  @Override
  public Object getRootElement() {
    return myRoot;
  }

  private final SimpleNode myRoot = new SimpleNode() {

    @NotNull
    @Override
    public SimpleNode[] getChildren() {
      final Module[] modules = ModuleManager.getInstance(MultiDomTreeStructure.this.myProject).getModules();
      final ArrayList<Module> webModules = new ArrayList<>();
      for (Module module : modules) {
        if (myFactory.getAllModels(module).size() > 0) {
          webModules.add(module);
        }
      }
      List<WebModuleNode> children = new ArrayList<WebModuleNode>(webModules.size());
      for (Module module : webModules) {
        children.add(new WebModuleNode(module));
      }
      return children.size() == 1 ? children.get(0).getChildren() : children.toArray(new SimpleNode[0]);
    }

    @Override
    public boolean isAutoExpandNode() {
      return true;
    }
  };

  @NotNull
  public List<DomFileElement<T>> getFileElements(M model) {

    final List<DomFileElement<T>> fileElements = myFactory.getFileElements(model);
    for (DomFileElement<T> fileElement : fileElements) {
      fileElement.getFile().putUserData(BaseDomElementNode.TREE_NODES_HIDERS_KEY, myHiders);
      fileElement.getFile().putUserData(BaseDomElementNode.CONSOLIDATED_NODES_KEY, myConsolidated);
      fileElement.getFile().putUserData(BaseDomElementNode.FOLDER_NODES_KEY, myFolders);
    }
    return fileElements;
  }

  protected static SimpleNode createFileNode(final DomFileElement fileElement) {
    return new FileElementNode(fileElement);
  }


  private class WebModuleNode extends SimpleNode {

    private final Module myModule;

    WebModuleNode(final Module module) {
      myModule = module;
      setUniformIcon(JavaeeIcons.WEB_MODULE_SMALL);
      setPlainText(module.getName());
    }

    @NotNull
    @Override
    public SimpleNode[] getChildren() {
      List<M> models = myFactory.getAllModels(myModule);
      List<ModelNode> children = new ArrayList<ModelNode>(models.size());
      for (M model : models) {
        children.add(new ModelNode(model));
      }

      if (children.size() == 1) {
        return children.get(0).getChildren();
      } else {
        return children.toArray(new SimpleNode[0]);
      }
    }

    @Override
    public boolean isAutoExpandNode() {
      return true;
    }
  }


  private class ModelNode extends SimpleNode {
    private final M myModel;

    ModelNode(M model) {
      myModel = model;
      setUniformIcon(StrutsApiIcons.StrutsModule);
      setPlainText(model.getName());
    }

    @NotNull
    @Override
    public SimpleNode[] getChildren() {
      List<DomFileElement<T>> elements = getFileElements(myModel);
      SimpleNode[] children = new SimpleNode[elements.size()];
      for (int i = 0; i < elements.size(); i++) {
        children[i] = createFileNode(elements.get(i));
      }
      return children;
    }

    @Override
    public boolean isAutoExpandNode() {
      return true;
    }
  }


  private static class FileElementNode extends DomFileElementNode {

    FileElementNode(final DomFileElement fileElement) {
      super(fileElement);
    }

    @Override
    @Nullable
    public String getNodeName() {
      return getDomElement().getFile().getName();
    }

    @Override
    public Icon getNodeIcon() {
      return getDomElement().getFile().getIcon(Iconable.ICON_FLAG_READ_STATUS);
    }
  }

}
