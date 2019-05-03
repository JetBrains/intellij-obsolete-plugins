package com.intellij.designer.inspector.impl;

import com.intellij.designer.inspector.Property;
import com.intellij.designer.inspector.PropertyInspector;
import com.intellij.ui.SpeedSearchBase;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author spleaner
 */
public class InspectorSpeedSearch extends SpeedSearchBase<TreeTable> {
  private final PropertyInspector myInspector;

  public InspectorSpeedSearch(final PropertyInspector tree) {
    super(tree);
    myInspector = tree;
  }

  @Override
  protected int getSelectedIndex() {
    return myInspector.getSelectedPropertyIndex();
  }

  @NotNull
  @Override
  protected Object[] getAllElements() {
    final int rowCount = myInspector.getRowCount();
    final List<Property> resultList = new ArrayList<>();
    for (int i = 0; i < rowCount; i++) {
      resultList.add((Property) myInspector.getPathForRow(i).getLastPathComponent());
    }

    return resultList.toArray(new Object[rowCount]);
  }

  @Override
  protected String getElementText(final Object element) {
    assert element instanceof Property;
    return ((Property)element).getName().toString();
  }

  @Override
  protected void selectElement(final Object element, final String selectedText) {
    assert element instanceof Property;
    myInspector.setSelectedProperty((Property) element);
  }

}
