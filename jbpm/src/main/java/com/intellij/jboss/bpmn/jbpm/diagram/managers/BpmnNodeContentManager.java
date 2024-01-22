package com.intellij.jboss.bpmn.jbpm.diagram.managers;

import com.intellij.diagram.AbstractDiagramNodeContentManager;
import com.intellij.diagram.DiagramBuilder;
import com.intellij.diagram.DiagramCategory;
import com.intellij.uml.utils.DiagramBundle;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class BpmnNodeContentManager extends AbstractDiagramNodeContentManager {
  public static final DiagramCategory DETAILS =
    new DiagramCategory(DiagramBundle.messagePointer("category.name.details"), PlatformIcons.PROPERTIES_ICON, true);

  private final static DiagramCategory[] CATEGORIES = {DETAILS};

  @Override
  public boolean isInCategory(@Nullable Object nodeElement, @Nullable Object item,
                              @NotNull DiagramCategory category,
                              @Nullable DiagramBuilder builder) {
    if (category.equals(DETAILS)) {
      return true;
    }
    return false;
  }

  @Override
  public DiagramCategory @NotNull [] getContentCategories() {
    return CATEGORIES;
  }
}
