package com.intellij.restClient;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.colors.impl.DelegateColorScheme;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.ui.*;
import com.intellij.util.ui.table.JBListTable;
import com.intellij.util.ui.table.JBTableRow;
import com.intellij.util.ui.table.JBTableRowEditor;
import com.intellij.util.ui.table.JBTableRowRenderer;
import com.intellij.httpClient.execution.RestClientRequest;
import com.michaelbaranov.microba.calendar.DatePicker;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieIdentityComparator;
import org.apache.http.impl.cookie.BasicClientCookie;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Dennis.Ushakov
 */
public class RESTCookiesPanel extends JPanel {
  private final CookieTableModel myTableModel;
  private final JBListTable myCookiesPanel;

  public RESTCookiesPanel(final Project project, Disposable parent) {
    super(new BorderLayout());
    setBorder(JBUI.Borders.empty(5));
    myTableModel = new CookieTableModel();
    final JBTable cookiesTable = new JBTable(myTableModel);
    myCookiesPanel = new CookiesTable(cookiesTable, project, parent);
    add(ToolbarDecorator.createDecorator(myCookiesPanel.getTable()).disableUpDownActions().createPanel(), BorderLayout.CENTER);
  }

  public void setCookies(Collection<Cookie> cookies) {
    myTableModel.setCookies(cookies);
    ((AbstractTableModel)myCookiesPanel.getTable().getModel()).fireTableDataChanged();
  }

  public void saveToRequest(RestClientRequest request) {
    for (BasicClientCookie cookie : myTableModel.myCookies) {
      request.addBiscuit(cookie);
    }
  }

  public void loadFromRequest(RestClientRequest request) {
    myTableModel.myCookies.clear();
    for (RestClientRequest.Biscuit biscuit : request.biscuits) {
      final BasicClientCookie cookie = new BasicClientCookie(biscuit.getName(), biscuit.getValue());
      cookie.setDomain(biscuit.getDomain());
      cookie.setPath(biscuit.getPath());
      if (biscuit.getDate() != -1) {
        cookie.setExpiryDate(new Date(biscuit.getDate()));
      }
      myTableModel.myCookies.add(cookie);
    }
    myTableModel.fireTableStructureChanged();
  }

  private static class CookieTableModel extends AbstractTableModel implements ItemRemovable, EditableModel {
    List<BasicClientCookie> myCookies = new ArrayList<>();
    private static final CookieIdentityComparator COMPARATOR = new CookieIdentityComparator();

    @Override
    public int getRowCount() {
      return myCookies.size();
    }

    @Override
    public int getColumnCount() {
      return 5;
    }

    @Override
    public Object getValueAt(int row, int column) {
      final Cookie cookie = myCookies.get(row);
      if (column == 0) {
        return cookie.getName();
      }
      if (column == 1) {
        return cookie.getValue();
      }
      if (column == 2) {
        return cookie.getDomain();
      }
      if (column == 3) {
        return cookie.getPath();
      }
      if (column == 4) {
        return cookie.getExpiryDate();
      }
      return null;
    }

    @Override
    public void setValueAt(Object o, int row, int column) {
      final BasicClientCookie cookie = myCookies.get(row);
      if (column == 0) {
        final BasicClientCookie replacement = new BasicClientCookie((String)o, cookie.getValue());
        replacement.setDomain(cookie.getDomain());
        replacement.setPath(cookie.getPath());
        replacement.setExpiryDate(cookie.getExpiryDate());
        myCookies.remove(row);
        myCookies.add(row, replacement);
      } else if (column == 1) {
        cookie.setValue((String)o);
      } else if (column == 2) {
        cookie.setDomain((String)o);
      } else if (column == 3) {
        cookie.setPath((String)o);
      } else if (column == 4) {
        cookie.setExpiryDate((Date)o);
      }
      fireTableCellUpdated(row, column);
    }

    @Override
    public void addRow() {
      myCookies.add(new BasicClientCookie("", ""));
      int row = myCookies.size() - 1;
      fireTableRowsInserted(row, row);
    }

    @Override
    public void exchangeRows(int oldIndex, int newIndex) {}

    @Override
    public boolean canExchangeRows(int oldIndex, int newIndex) {
      return false;
    }

    @Override
    public void removeRow(int idx) {
      myCookies.remove(idx);
      fireTableRowsDeleted(idx, idx);
    }

    public Cookie getCookie(int row) {
      return myCookies.get(row);
    }

    public void setCookies(Collection<Cookie> cookies) {
      for (final Cookie cookie : cookies) {
        final int index = ContainerUtil.indexOf(myCookies,
                                                ownCookie -> COMPARATOR.compare(cookie, ownCookie) == 0);
        if (index >= 0) {
          myCookies.remove(index);
          myCookies.add(index, (BasicClientCookie)cookie);
        } else {
          myCookies.add((BasicClientCookie)cookie);
        }
      }
      fireTableStructureChanged();
    }
  }

  private int getMaxLength(int column, int forcedMax) {
    int max = 0;
    for (int i = 0; i < myTableModel.getRowCount(); i++) {
      final Object at = myTableModel.getValueAt(i, column);
      final String text = at instanceof Date ? DateFormatUtil.formatDateTime((Date)at) : (String)at;
      max = Math.max(max, text != null ? text.length() : 4);
    }
    return Math.min(max, forcedMax);
  }

  private static String cutIfLong(String string, int size) {
    return string.length() > size ? string.substring(0, size - 1) + "\u2026" : string;
  }

  private class CookiesTable extends JBListTable {
    private final Project myProject;

    CookiesTable(JBTable cookiesTable, Project project, Disposable parent) {
      super(cookiesTable, parent);
      myProject = project;
    }

    @Override
    protected JBTableRowRenderer getRowRenderer(int row) {
      //TODO do not create an EditorTextField upon each paint - use com.intellij.ui.EditorTextFieldCellRenderer or something similar
      return new JBTableRowRenderer() {
        @Override
        public JComponent getRowRendererComponent(JTable table, int row, boolean selected, boolean focused) {
          final Cookie cookie = ((CookieTableModel)table.getModel()).getCookie(row);
          final String name = cookie.getName();
          final String value = cutIfLong(cookie.getValue(), 100);
          final String domain = cookie.getDomain() != null ? cookie.getDomain() : "none";
          final String path = cutIfLong(cookie.getPath() != null ? cookie.getPath() : "none", 20);
          final Date date = cookie.getExpiryDate();
          String text = " Name: " + name + StringUtil.repeat(" ", getMaxLength(0, 100) - name.length()) + "   ";
          text += "Domain: " + domain + StringUtil.repeat(" ", getMaxLength(2, 100) - domain.length()) + "   ";
          text += "Path: " + path + StringUtil.repeat(" ", getMaxLength(3, 20) - path.length()) + "   ";
          text += "Expires: " + (date != null ? DateFormatUtil.formatDateTime(date) : "session") + "\n";
          text += " Value: " + value;
          final JComponent renderer = JBListTable.createEditorTextFieldPresentation(myProject, PlainTextFileType.INSTANCE, text, selected, focused);
          ((EditorTextField)renderer.getComponent(0)).addSettingsProvider(editor -> {
            editor.setOneLineMode(false);
            editor.setColorsScheme(new DelegateColorScheme(editor.getColorsScheme()) {
              @Override
              public float getLineSpacing() {
                return 1.2f;
              }
            });
          });
          final Dimension size = renderer.getPreferredSize();
          renderer.setPreferredSize(new Dimension(size.width, size.height * 2));

          final JPanel result = new JPanel(new BorderLayout());
          final JPanel rendererPanel = new JPanel(new BorderLayout()) {
            @Override
            public void setBackground(Color color) {
              super.setBackground(color);
              renderer.setBackground(color);
              for (Component child : renderer.getComponents()) {
                child.setBackground(color);
              }
            }
          };
          rendererPanel.add(renderer, BorderLayout.CENTER);
          result.add(rendererPanel, BorderLayout.CENTER);
          final JLabel rowLabel = new JLabel("  " + (row + 1) + ". ");
          if (!StartupUiUtil.isUnderDarcula()) {
            rowLabel.setForeground(UIUtil.getInactiveTextColor());
          }
          result.add(rowLabel, BorderLayout.WEST);
          return result;
        }
      };
    }

    @Override
    protected JBTableRowEditor getRowEditor(final int row) {
      return new JBTableRowEditor() {
        private EditorTextField myNameEditor;
        private EditorTextField myValueEditor;
        private EditorTextField myDomainEditor;
        private EditorTextField myPathEditor;
        private DatePicker myDatePicker;

        @Override
        public void prepareEditor(JTable table, int row) {
          setLayout(new GridLayout(1, 4));
          myNameEditor = createEditor(row, 0, "Name:");
          myValueEditor = createEditor(row, 1, "Value:");
          myDomainEditor = createEditor(row, 2, "Domain:");
          myPathEditor = createEditor(row, 3, "Path:");
          myDatePicker = new DatePicker((Date)myTableModel.getValueAt(row, 4), DateFormatUtil.getDateTimeFormat().getDelegate()) {
            @Override
            public void removeNotify() {
              stopEditing();
              super.removeNotify();
            }
          };
          myDatePicker.setKeepTime(true);
          myDatePicker.setStripTime(false);
          add(createLabeledPanel("Expiry date:", myDatePicker));
        }

        private EditorTextField createEditor(int row, int column, String name) {
          final String value = (String)myTableModel.getValueAt(row, column);
          final EditorTextField field = new EditorTextField(value != null ? value : "");
          field.addDocumentListener(new RowEditorChangeListener(column));
          add(createLabeledPanel(name, field));
          return field;
        }

        @Override
        public JBTableRow getValue() {
          return new JBTableRow() {
            @Override
            public Object getValueAt(int column) {
              if (column == 0) {
                return myNameEditor.getText().trim();
              } else if (column == 1) {
                return myValueEditor.getText().trim();
              }
              else if (column == 2) {
                return myDomainEditor.getText().trim();
              }
              else if (column == 3) {
                return myPathEditor.getText().trim();
              }
              else if (column == 4) {
                return myDatePicker.getDate();
              }
              throw new IllegalArgumentException("No such column " + column);
            }
          };
        }

        @Override
        public JComponent getPreferredFocusedComponent() {
          String cookieName = row >= myTableModel.getRowCount() ? null : (String)myTableModel.getValueAt(row, 0);
          return StringUtil.isEmpty(cookieName) ? myNameEditor.getFocusTarget() : myValueEditor.getFocusTarget();
        }

        @Override
        public JComponent[] getFocusableComponents() {
          return new JComponent[] {
            myNameEditor.getFocusTarget(),
            myValueEditor.getFocusTarget(),
            myDomainEditor.getFocusTarget(),
            myPathEditor.getFocusTarget()
          };
        }
      };
    }
  }
}