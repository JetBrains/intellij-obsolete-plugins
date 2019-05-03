/*
 * Copyright (c) 2005 JetBrains s.r.o. All Rights Reserved.
 */
package com.intellij.designer.inspector.impl;

import com.intellij.designer.inspector.PresentationManager;
import com.intellij.designer.inspector.PropertyInspector;
import com.intellij.designer.inspector.RenderingContext;

/**
 * @author spleaner
 */
public class RenderingContextImpl implements RenderingContext {
  private final PropertyInspector myInspector;
  private final boolean mySelected;
  private final boolean myFocused;
  private final boolean myExpanded;
  private final boolean myEditor;

  public RenderingContextImpl(final PropertyInspector inspector,
                              final boolean editor,
                              final boolean selected,
                              final boolean focused,
                              boolean expanded) {
    myInspector = inspector;
    myEditor = editor;
    mySelected = selected;
    myFocused = focused;
    myExpanded = expanded;
  }

  @Override
  public boolean isExpanded() {
    return myExpanded;
  }

  @Override
  public PropertyInspector getInspector() {
    return myInspector;
  }

  @Override
  public boolean isSelected() {
    return mySelected;
  }

  @Override
  public boolean hasFocus() {
    return myFocused;
  }

  @Override
  public PresentationManager getPresentationManager() {
    return getInspector().getPresentationManager();
  }

  @Override
  public boolean isEditor() {
    return myEditor;
  }
}
