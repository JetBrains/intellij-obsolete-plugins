/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.propertyInspector;

import com.intellij.designer.inspector.PropertyInspector;
import com.intellij.designer.inspector.PropertyRenderer;
import com.intellij.designer.inspector.RenderingContext;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.highlighting.DomElementAnnotationsManager;
import com.intellij.util.xml.highlighting.DomElementProblemDescriptor;
import com.intellij.util.xml.highlighting.DomElementsProblemsHolder;
import com.intellij.util.xml.ui.BaseControl;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class DomPropertyRenderer implements PropertyRenderer<DomProperty> {

  private final BooleanTableCellRenderer myBooleanRenderer = new BooleanTableCellRenderer();
  private final DefaultTableCellRenderer myTextRenderer = new DefaultTableCellRenderer();

  public DomPropertyRenderer() {
    myBooleanRenderer.setHorizontalAlignment(JLabel.LEFT);
    myBooleanRenderer.setBorder(new EmptyBorder(0, 1, 0, 0));
    myTextRenderer.setBorder(JBUI.Borders.empty());
  }

  @Override
  public JComponent getRendererComponent(@NotNull final DomProperty property, @NotNull final RenderingContext context) {

    Class clazz = property.getObjectClass();
    TableCellRenderer renderer;
    Object val = property.getValueObject();
    if (clazz == Boolean.class || clazz == boolean.class) {
      renderer = myBooleanRenderer;
    } else {
      renderer = myTextRenderer;
    }
    boolean isSelected = context.isSelected();
    boolean hasErrors = false;
//    boolean hasWarnings = false;

    final GenericDomValue value = property.getDomValue();
    if (value.isValid()) {
      final DomElementAnnotationsManager annotationsManager = DomElementAnnotationsManager.getInstance(value.getManager().getProject());
      final DomElementsProblemsHolder holder = annotationsManager.getCachedProblemHolder(value);
      final List<DomElementProblemDescriptor> errorProblems = holder.getProblems(value);
      hasErrors = errorProblems.size() > 0;
/*
      final List<DomElementProblemDescriptor> warningProblems = holder.getProblems(value, true, true, HighlightSeverity.WARNING);
      hasWarnings = warningProblems.size() > 0;
*/
    }


    final PropertyInspector table = context.getInspector();
    JComponent component = (JComponent)renderer.getTableCellRendererComponent(table, val, isSelected, context.hasFocus(), 0, 0);
    if (hasErrors) {
      component.setForeground(JBColor.RED);
    }
    else {
      component.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
    }

    // highlight empty cell with errors
    if (hasErrors && (val == null || val.toString().trim().length() == 0)) {
      component.setBackground(BaseControl.ERROR_BACKGROUND);
    }
/*
    else if (hasWarnings) {
      component.setBackground(BaseControl.WARNING_BACKGROUND);
      if (isSelected) component.setForeground(Color.BLACK);
    }
*/
    else {
      component.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
    }

    return component;
  }

  @Override
  public boolean accepts(final DomProperty property) {
    return true;
  }
}
