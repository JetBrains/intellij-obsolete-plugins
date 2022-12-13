package com.intellij.dmserver.libraries.obr;

import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class GrayableTableWrapper<T, C extends Column<T>> extends JTableWrapper<T, C> {

  public GrayableTableWrapper(C[] columns) {
    super(new JBTable() {

      @NotNull
      @Override
      public Component prepareRenderer(@NotNull TableCellRenderer
                                         renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        Color background = c.getBackground();
        if (background == null || !background.equals(getSelectionBackground())) {
          c.setBackground(((GrayableTableModel)getModel()).isCellEnabled(row, column) ? getBackground() : UIUtil.getPanelBackground());
        }
        return c;
      }
    }, columns);
  }

  @Override
  protected GrayableTableModelBase createTableModel() {
    return new GrayableTableModelBase();
  }

  private interface GrayableTableModel {

    boolean isCellEnabled(int row, int column);
  }

  protected class GrayableTableModelBase extends JTableWrapperModel implements GrayableTableModel {

    @Override
    public boolean isCellEnabled(int row, int column) {
      return true;
    }
  }
}
