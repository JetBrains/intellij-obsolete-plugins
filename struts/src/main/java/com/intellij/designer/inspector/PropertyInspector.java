/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.designer.inspector;

import com.intellij.designer.inspector.impl.InspectorSpeedSearch;
import com.intellij.designer.inspector.impl.NameRendererAdapter;
import com.intellij.designer.inspector.impl.ValueEditorAdapter;
import com.intellij.designer.inspector.impl.ValueRendererAdapter;
import com.intellij.designer.inspector.impl.actions.CollapseAllAction;
import com.intellij.designer.inspector.impl.actions.ExpandAllAction;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.ui.treeStructure.treetable.ListTreeTableModel;
import com.intellij.ui.treeStructure.treetable.TreeColumnInfo;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.table.*;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

/**
 * @author spleaner
 */
public abstract class PropertyInspector extends TreeTable implements DataProvider {
  @NonNls
  public static final String INSPECTOR_KEY = "propertyInspector";
  @NonNls
  public static final String STOP_EDITING_ACTION_KEY = "stpEditingOnEnter";

  private PresentationManager myPresentationManager;
  private boolean myStopEditingOnEnter = true;
  private boolean myStartEditingOnEnter = true;
  private final Map<Class, PropertyRenderer> myClass2NameRenderer = new HashMap<>();
  private final Map<Class, PropertyRenderer> myClass2ValueRenderer = new HashMap<>();
  private final Map<Class, PropertyEditor> myClass2ValueEditor = new HashMap<>();

  public PropertyInspector() {
    super(createEmptyModel());

    setRowMargin(0);

    new InspectorSpeedSearch(this);

    myPresentationManager = new DefaultPresentationManager<>(this);

    addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (ComboBox.TABLE_CELL_EDITOR_PROPERTY.equals(evt.getPropertyName()) && evt.getNewValue() == null && evt.getOldValue() != null) {
         final Object editor = evt.getOldValue();
         if (editor instanceof ValueEditorAdapter) {
           ((ValueEditorAdapter)editor).cancelCellEditing();
           requestFocusInWindow();
         }
       }
      }
    });

    // IDEA-8141
    setDragEnabled(false);
    //setUI(new BasicTreeUI());   // In WindowsXP UI handles are not shown :(
  }

  public <P extends Property> void setNameRenderer(final Class<P> clazz, @NotNull final PropertyRenderer<P> renderer) {
    myClass2NameRenderer.put(clazz, renderer);
  }

  @SuppressWarnings({"unchecked"})
  public <P extends Property> PropertyRenderer<P> getNameRenderer(final P p) {
    return getValueFromMap(myClass2NameRenderer, p);
  }

  public <P extends Property> void setValueRenderer(final Class<P> clazz, @NotNull final PropertyRenderer<P> renderer) {
    myClass2ValueRenderer.put(clazz, renderer);
  }

  @SuppressWarnings({"unchecked"})
  public <P extends Property> PropertyRenderer<P> getValueRenderer(final P p) {
    return getValueFromMap(myClass2ValueRenderer, p);
  }

  public <P extends Property> void setValueEditor(final Class<P> clazz, @NotNull final PropertyEditor<P> editor) {
    myClass2ValueEditor.put(clazz, editor);
  }

  @SuppressWarnings({"unchecked"})
  public <P extends Property> PropertyEditor<P> getValueEditor(@NotNull final P p) {
    return getValueFromMap(myClass2ValueEditor, p);
  }

  private static <P extends Property, T extends PropertyValidator<P>> T getValueFromMap(@NotNull final Map<Class, T> class2valueMap, @NotNull final P p) {
    final Class<? extends Property> keyClass = p.getClass();

    T result = class2valueMap.get(keyClass);
    if (result != null) {
      return result;
    }

    for (Class c : class2valueMap.keySet()) {
      final T validator = class2valueMap.get(c);
      if (c.isAssignableFrom(keyClass) && validator.accepts(p)) {
        class2valueMap.put(keyClass, validator);
        return validator;
      }
    }

    return result;
  }

  protected String getNameColumnTitle() {
    return "name";
  }

  protected String getValueColumnTitle() {
    return "value";
  }

  protected ColumnInfo[] getColumnInfo() {
    return new ColumnInfo[]{ new TreeColumnInfo(getNameColumnTitle()),
      new ColumnInfo<Property, Object>(getValueColumnTitle()) {
        @Override
        public Object valueOf(final Property item) {
          return item;
        }

        @Override
        public boolean isCellEditable(final Property item) {
          final PropertyEditor<Property> editor = getValueEditor(item);
          return editor != null && editor.canEdit(item);
        }
      }};
  }

  protected void setStopEditingOnEnter(boolean b) {
    myStopEditingOnEnter = b;
  }

  protected void setStartEditingOnEnter(boolean b) {
    myStartEditingOnEnter = b;
  }

  public void setRoot(final Property root) {
    final int selected = getSelectedRow();
    setModel(new ListTreeTableModel(root, getColumnInfo()));
    if (getRowCount() > selected) {
      getSelectionModel().setSelectionInterval(selected,  selected);
    }
  }

  @Nullable
  public Property getRoot() {
    return (Property)getTableModel().getRoot();
  }

  /**
   * TODO[alexey.pegov]: review
   *
   * This hack is for EditorTextField which should be added to the container before it will be asked for
   * it's preferred size (real editor is created on addNotify() =( ).
   *
   * @see BasicTreeUI#startEditing(TreePath, MouseEvent)
   */
  @Override
  public boolean editCellAt(int row, int column, EventObject e) {
    if (!isEditing()) { // todo: stop editing if needed
      final TableCellEditor editor = getCellEditor(row, column);
      if (editor != null && isCellEditable(row, column) /*&& cellEditor.isCellEditable(null) */) {
        final Component c = editor.getTableCellEditorComponent(this, getRowValue(row), isRowSelected(row), row, column);
        add(c);
      }
    }

    return super.editCellAt(row, column, e);
  }

  //public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
  //  final Component c = super.prepareRenderer(renderer, row, column);
  //  final CellRendererPane pane = getPane();
  //  pane.add(c);
  //  return c;
  //}
  //
  //public CellRendererPane getPane() {
  //  if (myRendererPane == null) {
  //  final int count = getComponentCount();
  //  for (int i = 0; i < count; i++) {
  //    final Component c = getComponent(i);
  //    if (c instanceof CellRendererPane) {
  //      myRendererPane = (CellRendererPane) c;
  //      break;
  //    }
  //  }
  //  }
  //
  //  return myRendererPane;
  //}

  // to not allow scrollpane to control width of the insecotor (to prevent clipping)
  @Override
  public boolean getScrollableTracksViewportWidth() {
    return true;
  }

  public Rectangle getCellRect(@NotNull final Property p) {
    final int index = getPropertyIndex(p);
    return getCellRect(index, 0, false);
  }

  @Override
  protected void processMouseEvent(MouseEvent e) {
    if (e.isPopupTrigger()) {
      proceedPopup(e);
    }

    super.processMouseEvent(e);
  }

  private void proceedPopup(MouseEvent e) {
    final ActionManager actionManager = ActionManager.getInstance();
    final DefaultActionGroup group = new DefaultActionGroup("???", true);

    addActions(group);

    final ActionPopupMenu popupMenu = actionManager.createActionPopupMenu(ActionPlaces.TODO_VIEW_POPUP, group);
    popupMenu.getComponent().show(this, e.getX(), e.getY());
  }

  protected void addActions(final DefaultActionGroup group) {
    group.add(ActionManager.getInstance().getAction("PropertyInspectorActions.CommonActions"));
  }

  @Override
  public void setModel(TreeTableModel treeTableModel) {
    TreeTableModel newModel = (treeTableModel == null) ? createEmptyModel() : treeTableModel;

    super.setModel(newModel);

    configure();

    setRootVisible(false);

    if (newModel != null && newModel.getRoot() instanceof Property) {
      expandAll();
    }
  }

  private void configure() {
    getTableHeader().setReorderingAllowed(false);
    setDragEnabled(false);
    getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    getTableHeader().setReorderingAllowed(false);

    final Tree tree = getTree();

    tree.setShowsRootHandles(false);
    //getTree().setCellRenderer(new JavaeeToolTipNodeRenderer());
    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

    tree.setCellRenderer(new NameRendererAdapter(this, tree.getCellRenderer()));

    final TableColumnModel model = getColumnModel();
    if (model.getColumnCount() == 2) {
      final TableColumn valueColumn = model.getColumn(1);
      valueColumn.setCellRenderer(new ValueRendererAdapter(this, getDefaultRenderer(Object.class)));
      valueColumn.setCellEditor(new ValueEditorAdapter(this));
    }
  }

  private static TreeTableModel createEmptyModel() {
    return new ListTreeTableModel(new AbstractProperty<String, String>("dummy", "dummy") {
    }, ColumnInfo.EMPTY_ARRAY);
  }

  @Override
  @Nullable
  public Object getData(@NotNull String dataId) {
    if (INSPECTOR_KEY.equals(dataId)) {
      return this;
    }

    return null;
  }

  public void expandAll() {
    TreeUtil.expandAll(getTree());
  }

  public void collapseAll() {
    TreeUtil.collapseAll(getTree(), 1);
  }

  public void firePropertyAdded(final Property added, final Property parent, final int index) {
    ((AbstractTableModel)getModel()).fireTableRowsInserted(index, index);
  }

  public void firePropertyRemoved(final Property property, final Property parent, final int index) {
    ((AbstractTableModel)getModel()).fireTableRowsDeleted(index, index);
  }

  @NotNull
  public AnAction getExpandAll() {
    return new ExpandAllAction(this);
  }

  @NotNull
  public AnAction getCollapseAll() {
    return new CollapseAllAction(this);
  }

  //public boolean isSelected(Property p) {
  //  return isPathSelected(TreeUtil.getPathFromRoot(p));
  //}

  @Nullable
  public Property getSelectedProperty() {
    final int selectedRow = getSelectedRow();
    return getRowValue(selectedRow);
  }

  protected Property getRowValue(final int row) {
    return (Property) getModel().getValueAt(row, 1);
  }

  public int getSelectedPropertyIndex() {
    return getSelectedRow();
  }

  protected int getPropertyIndex(@NotNull final Property p) {
    final TableModel model = getModel();
    for (int i = 0; i < model.getRowCount(); i++) {
      final Object o = model.getValueAt(i, 1);
      if (o == p) {
        return i;
      }
    }

    return -1;
  }

  public void setSelectedProperty(@NotNull Property p) {
    final int index = getPropertyIndex(p);
    getSelectionModel().setSelectionInterval(index, index);
  }

  public PresentationManager getPresentationManager() {
    return myPresentationManager;
  }

  public void setPresentationManager(PresentationManager manager) {
    myPresentationManager = manager;
  }

  public TreePath getPathForRow(final int row) {
    return getTree().getPathForRow(row);
  }

  public void processEnter() {
    if (myStopEditingOnEnter && isEditing()) {
      // ok, will stop the edit
      final TableCellEditor editor = getCellEditor();
      if (editor != null) {
        editor.stopCellEditing();
      }

      requestFocusInWindow();
    } else if (myStartEditingOnEnter) {
      // ok, will start it :)
      final int row = getSelectedRow();
      if (isCellEditable(row, 1)) {
        editCellAt(row, 1, null);
      }
    }
  }
}
