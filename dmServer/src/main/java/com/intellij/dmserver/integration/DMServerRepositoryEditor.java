package com.intellij.dmserver.integration;

import com.intellij.dmserver.install.ServerVersionHandler;
import com.intellij.dmserver.libraries.obr.Column;
import com.intellij.dmserver.libraries.obr.JTableWrapper;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DMServerRepositoryEditor {

  private static final RepositoryItem10ColumnBase[] our10Columns = new RepositoryItem10ColumnBase[]{

    new RepositoryItem10ColumnBase(DmServerBundle.message("Form.DMServerRepositoryEditor.repository.item.column.10.path")) {

      @Override
      public Object getColumnValue(DMServerRepositoryItem10 row) {
        return row.getPath();
      }
    }
  };

  private static final RepositoryItem20ColumnBase[] our20Columns = new RepositoryItem20ColumnBase[]{

    new RepositoryItem20ColumnBase(DmServerBundle.message("Form.DMServerRepositoryEditor.repository.item.column.20.type")) {

      @Override
      public Object getColumnValue(DMServerRepositoryItem20Base row) {
        return row.getTypePropertyValue();
      }

      @Override
      public boolean needPack() {
        return true;
      }
    },
    new RepositoryItem20ColumnBase(DmServerBundle.message("Form.DMServerRepositoryEditor.repository.item.column.20.name")) {

      @Override
      public Object getColumnValue(DMServerRepositoryItem20Base row) {
        return row.getName();
      }

      @Override
      public boolean needPack() {
        return true;
      }
    },
    new RepositoryItem20ColumnBase(DmServerBundle.message("Form.DMServerRepositoryEditor.repository.item.column.20.path")) {

      @Override
      public Object getColumnValue(DMServerRepositoryItem20Base row) {
        return row.getPath();
      }
    }
  };

  private final JPanel myMainPanel;
  private final JBTable myPathTable;

  private DMServerIntegrationEditor myParent;

  private final List<Behavior<?>> myPossibleBehaviors = Arrays.asList(new Behavior10(), new Behavior20());

  private Behavior<?> myBehavior;

  private DMServerRepositoryEditorListener myListener;

  public DMServerRepositoryEditor() {
    myPathTable = new JBTable();
    myPathTable.setShowGrid(false);

    myMainPanel = ToolbarDecorator.createDecorator(myPathTable)
      .setAddAction(new AnActionButtonRunnable() {
        @Override
        public void run(AnActionButton button) {
          myBehavior.onAdd();
        }
      }).setEditAction(new AnActionButtonRunnable() {
        @Override
        public void run(AnActionButton button) {
          myBehavior.onEdit();
        }
      }).setRemoveAction(new AnActionButtonRunnable() {
        @Override
        public void run(AnActionButton button) {
          myBehavior.onRemove();
        }
      }).disableUpDownActions().createPanel();
  }

  public void setListener(DMServerRepositoryEditorListener listener) {
    myListener = listener;
  }

  private int getSelectedRow() {
    return myPathTable.getSelectedRow();
  }

  private void setSelectedRow(int row) {
    myPathTable.getSelectionModel().setSelectionInterval(row, row);
  }

  public void applyTo(DMServerIntegrationData data) {
    data.setRepositoryItems(getRepositoryItems());
  }

  public void loadFrom(DMServerIntegrationData data, ServerVersionHandler.DMVersion version) {
    for (Behavior<?> behavior : myPossibleBehaviors) {
      if (behavior.getVersion() == version) {
        myBehavior = behavior;
        myBehavior.activate(data);
        return;
      }
    }
    myBehavior = null;
  }

  public void setParent(DMServerIntegrationEditor parent) {
    myParent = parent;
  }

  public DMServerIntegrationEditor getParent() {
    return myParent;
  }

  public List<DMServerRepositoryItem> getRepositoryItems() {
    return myBehavior == null
           ? Collections.emptyList()
           : new ArrayList<>(myBehavior.getRepositoryItems());
  }

  public void setEnabled(boolean enabled) {
    myPathTable.setEnabled(enabled);
  }

  public JComponent createComponent() {
    return myMainPanel;
  }

  private static abstract class RepositoryItem20ColumnBase extends Column<DMServerRepositoryItem20Base> {

    RepositoryItem20ColumnBase(String name) {
      super(name);
    }
  }

  private static abstract class RepositoryItem10ColumnBase extends Column<DMServerRepositoryItem10> {

    RepositoryItem10ColumnBase(String name) {
      super(name);
    }
  }

  private abstract class Behavior<T extends DMServerRepositoryItem> {

    private JTableWrapper<T, Column<T>> myTableWrapper;

    private final List<T> myRepositoryItems = new ArrayList<>();

    public List<T> getRepositoryItems() {
      return myRepositoryItems;
    }

    public abstract ServerVersionHandler.DMVersion getVersion();

    public abstract DMServerRepositoryFolderDialog<T> createItemDialog();

    public void activate(DMServerIntegrationData data) {
      myTableWrapper = createTableWrapper();
      myRepositoryItems.clear();
      for (DMServerRepositoryItem repositoryItem : data.getRepositoryItems()) {
        if (getItemClass().isInstance(repositoryItem)) {
          myRepositoryItems.add(getItemClass().cast(repositoryItem));
        }
      }
      updateTable();
    }

    public void updateTable() {
      myTableWrapper.setInputRows(myRepositoryItems);
    }

    public void onAdd() {
      DMServerRepositoryFolderDialog<T> itemDialog = createItemDialog();
      if (!itemDialog.showAndGet()) {
        return;
      }

      myRepositoryItems.add(itemDialog.getItem());
      updateTable();
      setSelectedRow(myRepositoryItems.size() - 1);

      if (myListener != null) {
        myListener.itemAdded();
      }
    }

    private void onEdit() {
      int selectedRow = getSelectedRow();
      if (selectedRow == -1) {
        return;
      }

      DMServerRepositoryFolderDialog<T> itemDialog = createItemDialog();
      itemDialog.setItem(myRepositoryItems.get(selectedRow));
      if (!itemDialog.showAndGet()) {
        return;
      }

      myRepositoryItems.set(selectedRow, itemDialog.getItem());
      updateTable();
      setSelectedRow(selectedRow);
    }

    private void onRemove() {
      int selectedRow = getSelectedRow();
      if (selectedRow == -1) {
        return;
      }

      myRepositoryItems.remove(selectedRow);
      updateTable();
    }

    protected abstract Class<T> getItemClass();

    protected abstract JTableWrapper<T, Column<T>> createTableWrapper();
  }

  private class Behavior20 extends Behavior<DMServerRepositoryItem20Base> {

    @Override
    public ServerVersionHandler.DMVersion getVersion() {
      return ServerVersionHandler.DMVersion.DM_20;
    }

    @Override
    protected JTableWrapper<DMServerRepositoryItem20Base, Column<DMServerRepositoryItem20Base>> createTableWrapper() {
      return new JTableWrapper<>(myPathTable, our20Columns);
    }

    @Override
    protected Class<DMServerRepositoryItem20Base> getItemClass() {
      return DMServerRepositoryItem20Base.class;
    }

    @Override
    public DMServerRepositoryFolderDialog<DMServerRepositoryItem20Base> createItemDialog() {
      return new DMServerRepositoryFolderDialog20(DMServerRepositoryEditor.this);
    }
  }

  private class Behavior10 extends Behavior<DMServerRepositoryItem10> {

    @Override
    public ServerVersionHandler.DMVersion getVersion() {
      return ServerVersionHandler.DMVersion.DM_10;
    }

    @Override
    protected JTableWrapper<DMServerRepositoryItem10, Column<DMServerRepositoryItem10>> createTableWrapper() {
      return new JTableWrapper<>(myPathTable, our10Columns);
    }

    @Override
    protected Class<DMServerRepositoryItem10> getItemClass() {
      return DMServerRepositoryItem10.class;
    }

    @Override
    public DMServerRepositoryFolderDialog<DMServerRepositoryItem10> createItemDialog() {
      return new DMServerRepositoryFolderDialog10(DMServerRepositoryEditor.this);
    }
  }
}
