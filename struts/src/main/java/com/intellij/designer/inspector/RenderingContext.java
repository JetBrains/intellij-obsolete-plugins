/*
 * Copyright (c) 2005 JetBrains s.r.o. All Rights Reserved.
 */
package com.intellij.designer.inspector;

/**
 * @author spleaner
 */
public interface RenderingContext {

  PropertyInspector getInspector();
  PresentationManager getPresentationManager();

  boolean isSelected();
  boolean hasFocus();

  boolean isEditor();

  boolean isExpanded();
}
