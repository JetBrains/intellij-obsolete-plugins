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

package com.intellij.struts.diagram;

import com.intellij.openapi.graph.builder.GraphBuilder;
import com.intellij.openapi.graph.builder.renderer.BasicGraphNodeRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;

import javax.swing.*;

/**
 * @author Dmitry Avdeev
 */
public class StrutsGraphNodeRenderer extends BasicGraphNodeRenderer<StrutsObject, StrutsObject>{

  public StrutsGraphNodeRenderer(GraphBuilder<StrutsObject, StrutsObject> builder, final Project project, final ModificationTracker tracker) {
    super(builder, tracker);
  }

  // TODO no need yet
//  protected Color getBackground(final StrutsObject node) {
//    return super.getBackground(node);
//  }

  @Override
  protected Icon getIcon(final StrutsObject node) {
    final Icon icon = node.getIcon();
    return icon == null ? super.getIcon(node) : icon;
  }

  @Override
  protected String getNodeName(final StrutsObject node) {
    return node.getName();
  }
}
