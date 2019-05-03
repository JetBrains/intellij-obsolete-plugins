/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
