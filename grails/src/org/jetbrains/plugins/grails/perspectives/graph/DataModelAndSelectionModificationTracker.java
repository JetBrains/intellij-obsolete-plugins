// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.perspectives.graph;

import com.intellij.openapi.graph.view.Graph2DSelectionEvent;
import com.intellij.openapi.graph.view.Graph2DSelectionListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.CompositeModificationTracker;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;

public class DataModelAndSelectionModificationTracker extends CompositeModificationTracker implements ModificationTracker, Graph2DSelectionListener {
  public DataModelAndSelectionModificationTracker(@NotNull Project project) {
    super(PsiManager.getInstance(project).getModificationTracker());
  }

  @Override
  public void onGraph2DSelectionEvent(final Graph2DSelectionEvent event) {
    incModificationCount();
  }
}

