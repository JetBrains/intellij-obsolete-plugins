/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.module.settings.general;

import com.intellij.j2meplugin.J2MEBundle;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.TableUtil;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;

public class UserKeysConfigurable {
  private final ListTableModel<UserDefinedOption> myUserDefinedOptions = new ListTableModel<>(PARAMETER_COLUMNS);
  private final TableView myTable = new TableView(myUserDefinedOptions);
  private JButton myAddButton;
  private JButton myRemoveButton;
  private JButton myMoveUpButton;
  private JButton myMoveDownButton;
  private JPanel myTablePlace;
  private JPanel myWholePanel;
  private final HashSet<? extends UserDefinedOption> myOptions;

  public UserKeysConfigurable(HashSet<? extends UserDefinedOption> userDefinedOptions) {
    myOptions = userDefinedOptions;
    myUserDefinedOptions.setSortable(false);
    myUserDefinedOptions.setItems(new ArrayList<>(myOptions));

    myRemoveButton.setEnabled(false);
    myMoveUpButton.setEnabled(false);
    myMoveDownButton.setEnabled(false);
    myTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (myTable.getSelectedRow() == -1) {
          myRemoveButton.setEnabled(false);
          myMoveUpButton.setEnabled(false);
          myMoveDownButton.setEnabled(false);
        }
        else {
          myRemoveButton.setEnabled(true);
          if (myTable.getSelectedRow() != 0) {
            myMoveUpButton.setEnabled(true);
          }
          else {
            myMoveUpButton.setEnabled(false);
          }
          if (myTable.getSelectedRow() != myUserDefinedOptions.getItems().size() - 1) {
            myMoveDownButton.setEnabled(true);
          }
          else {
            myMoveDownButton.setEnabled(false);
          }
        }
      }
    });

    myAddButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        myTable.stopEditing();
        ArrayList<UserDefinedOption> options = new ArrayList<>(myUserDefinedOptions.getItems());
        options.add(new UserDefinedOption("", ""));
        myUserDefinedOptions.setItems(options);
        TableUtil.selectRows(myTable, new int []{myTable.getRowCount() - 1});
      }
    });

    myRemoveButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        TableUtil.removeSelectedItems(myTable);
      }
    });


    myMoveDownButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        TableUtil.moveSelectedItemsDown(myTable);
      }
    });

    myMoveUpButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        TableUtil.moveSelectedItemsUp(myTable);
      }
    });
    myTablePlace.setLayout(new BorderLayout());
    myTablePlace.add(ScrollPaneFactory.createScrollPane(myTable), BorderLayout.CENTER);

  }

  public JPanel getUserKeysPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(myWholePanel, BorderLayout.CENTER);
    return panel;
  }

  public ListTableModel<UserDefinedOption> getUserDefinedOptions() {
    return (ListTableModel<UserDefinedOption>)myTable.getModel();
  }

  public void setUserDefinedOptions(ArrayList<UserDefinedOption> userDefinedOptions) {
    myUserDefinedOptions.setItems(userDefinedOptions);
  }

  public TableView getTable() {
    return myTable;
  }

  private static final ColumnInfo[] PARAMETER_COLUMNS = new ColumnInfo[]{
    new ColumnInfo<UserDefinedOption, String>(J2MEBundle.message("module.settings.user.defined.key")) {
      @Override
      public String valueOf(final UserDefinedOption userOption) {
        return userOption.getKey();
      }

      @Override
      public void setValue(final UserDefinedOption userOption, final String name) {
        userOption.setKey(name);
      }

      @Override
      public boolean isCellEditable(final UserDefinedOption userOption) {
        return true;
      }
    },
    new ColumnInfo<UserDefinedOption, String>(J2MEBundle.message("module.settings.user.defined.value")) {
      @Override
      public String valueOf(final UserDefinedOption userOption) {
        return userOption.getValue();
      }

      @Override
      public void setValue(final UserDefinedOption userOption, final String value) {
        userOption.setValue(value);
      }

      @Override
      public boolean isCellEditable(final UserDefinedOption userOption) {
        return true;
      }
    }
  };
}
