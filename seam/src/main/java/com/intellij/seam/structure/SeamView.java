/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.intellij.seam.structure;

import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.jam.view.tree.JamNodeDescriptor;
import com.intellij.jam.view.tree.JamTreeParameters;
import com.intellij.javaee.ui.JavaeeToolWindowViewBase;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.seam.HelpID;
import com.intellij.seam.facet.SeamFacet;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.TreeSelectionModel;

public final class SeamView extends JavaeeToolWindowViewBase {
  public SeamView(@NotNull Project project, @NotNull Disposable parentDisposable) {
    super(project, new JavaeeFrameworkViewTreeStructureProvider<>(SeamFacet.FACET_TYPE_ID) {
      @Override
      protected JamNodeDescriptor createFacetNodeDescriptor(SeamFacet facet,
                                                            NodeDescriptor<?> parentDescriptor,
                                                            JamTreeParameters parameters) {
        return new SeamFacetNodeDescriptor(project, facet, parentDescriptor, parameters);
      }
    }, parentDisposable);
    initComponents();
    myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
  }

  @Override
  protected String getHelpId() {
    return HelpID.SEAM_VIEW;
  }
}
