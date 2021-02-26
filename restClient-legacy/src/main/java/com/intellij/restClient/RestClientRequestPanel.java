package com.intellij.restClient;

import com.intellij.icons.AllIcons;
import com.intellij.microservices.http.HttpHeadersDictionary;
import com.intellij.microservices.mime.MimeTypes;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.*;
import com.intellij.ui.border.IdeaTitledBorder;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.table.JBTable;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.table.*;
import com.intellij.httpClient.execution.RestClientFormBodyPart;
import com.intellij.httpClient.execution.RestClientRequest;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author yole
 */
@SuppressWarnings("HardCodedStringLiteral") // shouldn't be visible at UI
public class RestClientRequestPanel extends JPanel implements Disposable {
  private final Project myProject;
  private final RestClientRequestPanel.ToggleSendParamsAction myToggleSendParamsAction;
  private JPanel myMainPanel;
  private boolean myDoNotSendParams;
  private JPanel myParametersPanel;
  private TextFieldWithBrowseButton myFile;
  private JLabel myFileLabel;
  private JBRadioButton myRbEmpty;
  private JBRadioButton myRbFileContents;
  private JBRadioButton myRbFileUpload;
  private JBRadioButton myRbText;
  private TextFieldWithBrowseButton myTextToSend;
  private JPanel myHeaderPanel;
  private JPanel myRequestBodyPanel;
  private final NameValueTableModel myHeaderData = new NameValueTableModel();
  private final NameValueTableModel myRequestData = new NameValueTableModel();
  private final JBListTable myHeaderTable;
  private final JBListTable myRequestTable;

  public static final @NonNls String ACCEPT = "Accept";
  public static final @NonNls String CONTENT_TYPE = "Content-Type";
  public static final @NonNls String CONTENT_LENGTH = "Content-Length";
  public static final @NonNls String CACHE_CONTROL = "Cache-Control";
  public static final @NonNls String[] IMMUTABLE_HEADER_PARAMS = {ACCEPT, CONTENT_LENGTH, CACHE_CONTROL};
  private String[] myMimeTypes;

  private void layoutMainPanel() {
    myMainPanel = new JPanel(new MigLayout("ins 3 0 0 0, fill"));

    myHeaderPanel = new JPanel(new BorderLayout());
    myParametersPanel = new JPanel(new BorderLayout());
    myRequestBodyPanel = new JPanel(new MigLayout("ins 0, fillx, flowy"));

    createMyFile();
    createMyTextToSend();

    myRbEmpty = new JBRadioButton("Empty");
    myRbText = new JBRadioButton("Text");
    myRbFileContents = new JBRadioButton("File contents");
    myRbFileUpload = new JBRadioButton("File upload (multipart/form-data)");
    myFileLabel = new JLabel("File to send:");

    myRequestBodyPanel.add(myRbEmpty);
    JComponent textPane = createGrowComponent(myRbText, myTextToSend);

    myRequestBodyPanel.add(textPane, "growx");
    myRequestBodyPanel.add(myRbFileContents);
    myRequestBodyPanel.add(myRbFileUpload);

    JComponent filePane = createGrowComponent(myFileLabel, myFile);

    myRequestBodyPanel.add(filePane, "gapbefore 19, growx");

    JBSplitter mainSplitter = new JBSplitter(false, 1f/3);
    JBSplitter rightSplitter = new JBSplitter(false, 1f/2);

    rightSplitter.setFirstComponent(myParametersPanel);
    rightSplitter.setSecondComponent(myRequestBodyPanel);

    mainSplitter.setFirstComponent(myHeaderPanel);
    mainSplitter.setSecondComponent(rightSplitter);

    myMainPanel.add(mainSplitter, "grow");
  }

  private JComponent createGrowComponent(JComponent label, JComponent component){
    JPanel panel = new JPanel(new MigLayout("ins 0, fillx", "[min!][grow]"));
    panel.add(label);
    panel.add(component, "growx");
    return panel;
  }

  public RestClientRequestPanel(final Project project, Disposable parent) {
    super(new BorderLayout());
    myProject = project;

    layoutMainPanel();

    add(myMainPanel, BorderLayout.CENTER);

    ActionListener listener = event -> updateFileEnabled();

    myRbEmpty.addActionListener(listener);
    myRbText.addActionListener(listener);
    myRbFileContents.addActionListener(listener);
    myRbFileUpload.addActionListener(listener);

    myMimeTypes = collectMimeTypes();

    final Insets insets = JBUI.insets(0, 0, 5, 3);
    myHeaderPanel.setBorder(new IdeaTitledBorder("  Headers", 5, insets));
    JBTable headerParameters = new JBTable(myHeaderData);
    myHeaderTable = new NameValueListTable(project, headerParameters, ": ", HttpHeadersDictionary.getHeaders().keySet(), parent);
    final JPanel panel = ToolbarDecorator.createDecorator(myHeaderTable.getTable())
      .disableUpDownActions()
      .createPanel();
    myHeaderPanel.add(panel, BorderLayout.CENTER);

    myHeaderData.setImmutableFields(IMMUTABLE_HEADER_PARAMS);
    myHeaderData.setImmutableHeaderExceptions(ACCEPT, CACHE_CONTROL);
    setHeader(ACCEPT, "*/*");
    //myHeaderData.addProperty(CONTENT_TYPE, "text/html");
    setHeader(CACHE_CONTROL, "no-cache");

    myParametersPanel.setBorder(new IdeaTitledBorder(" Request Parameters", 2, insets));
    JBTable parameters = new JBTable(myRequestData);
    parameters.setModel(myRequestData);
    myRequestTable = new NameValueListTable(project, parameters, "=", null, parent);
    myToggleSendParamsAction = new ToggleSendParamsAction();
    final JPanel requestPanel = ToolbarDecorator.createDecorator(myRequestTable.getTable())
      .disableUpDownActions()
      .addExtraAction(myToggleSendParamsAction)
      .createPanel();
    myParametersPanel.add(requestPanel, BorderLayout.CENTER);

    myRequestBodyPanel.setBorder(new IdeaTitledBorder("Request Body", 0, insets));
    Disposer.register(parent, this);
  }

  public void setCanHasBody(boolean canHasBody) {
    if (!canHasBody) {
      myRbEmpty.setSelected(true);
    }
    myRbEmpty.setEnabled(canHasBody);
    myRbText.setEnabled(canHasBody);
    myRbFileContents.setEnabled(canHasBody);
    myRbFileUpload.setEnabled(canHasBody);
  }

  void updateFileEnabled() {
    boolean canHasFile = myRbFileUpload.isSelected() || myRbFileContents.isSelected();
    myFile.setEnabled(canHasFile);
    myFileLabel.setEnabled(canHasFile);
    myTextToSend.setEnabled(myRbText.isSelected());
  }

  public void updateHeaderEditor() {
    myMimeTypes = collectMimeTypes();
  }

  public void saveToRequest(RestClientRequest request) {
    collectKeyValuePairs(myHeaderData, request.headers);
    collectKeyValuePairs(myRequestData, request.parameters);
    request.parametersEnabled = !myDoNotSendParams;
    request.haveTextToSend = myRbText.isSelected();
    request.haveFileToSend = myRbFileContents.isSelected() || myRbFileUpload.isSelected();
    request.textToSend = myTextToSend.getText();
    request.filesToSend = myFile.getText();

    if (myRbFileUpload.isSelected()) {
      request.isFileUpload = true;
      request.formBodyPart = ContainerUtil.map(request.getFiles(),
                                               file -> RestClientFormBodyPart.create(file.getName(), file));
    }
    else {
      request.isFileUpload = false;
      request.formBodyPart = ContainerUtil.emptyList();
    }
  }

  public void loadFromRequest(RestClientRequest request) {
    stopEditing();
    applyKeyValuePairs(myHeaderData, request.headers, myHeaderTable);
    applyKeyValuePairs(myRequestData, request.parameters, myRequestTable);
    setDoNotSendParams(!request.parametersEnabled);

    if (request.haveTextToSend) {
      myRbText.setSelected(true);
      myTextToSend.setText(request.textToSend);
    }
    else if (request.haveFileToSend) {
      myFile.setText(request.filesToSend);
      if (request.isFileUpload) {
        myRbFileUpload.setSelected(true);
      }
      else {
        myRbFileContents.setSelected(true);
      }
    }
    else {
      myRbEmpty.setSelected(true);
    }
  }

  private void setDoNotSendParams(boolean enabled) {
    myDoNotSendParams = enabled;
    myRequestTable.getTable().setEnabled(!myDoNotSendParams);
    myToggleSendParamsAction.setEnabled(true);
  }

  private void createMyTextToSend() {
    myTextToSend = new TextFieldWithBrowseButton(event -> {
      ArrayList<RestClientRequest.KeyValuePair> list = new ArrayList<>();
      collectKeyValuePairs(myHeaderData, list);
      String mimeType = "";
      for (RestClientRequest.KeyValuePair pair : list) {
        if (CONTENT_TYPE.equals(pair.getKey())) {
          mimeType = pair.getValue();
          break;
        }
      }
      final VirtualFile file = RestClientFileUtil.createFile(myTextToSend.getText(), RestClientFileUtil.findFileType(mimeType));
      final Document document = FileDocumentManager.getInstance().getDocument(file);
      assert document != null : "Failed to get document for " + mimeType;
      final Editor editor = EditorFactory.getInstance().createEditor(document, myProject, file, false);
      final DialogBuilder builder = new DialogBuilder(myProject);
      builder.setDimensionServiceKey("RestClientTextRequest");
      builder.setTitle("Specify the text to send:");
      editor.getComponent().setPreferredSize(new Dimension(400, 300));
      builder.setCenterPanel(editor.getComponent());
      builder.setPreferredFocusComponent(editor.getContentComponent());
      builder.addOkAction();
      builder.setOkOperation(() -> {
        myTextToSend.setText(document.getText());
        builder.getDialogWrapper().close(DialogWrapper.OK_EXIT_CODE);
      });
      builder.addCancelAction();
      builder.show();
      RestClientFileUtil.deleteFile(file);
      EditorFactory.getInstance().releaseEditor(editor);
    }, this);
  }

  private void createMyFile() {
    myFile = new TextFieldWithBrowseButton(e -> {
      final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, true, true, false, true);
      final VirtualFile[] files = FileChooser.chooseFiles(descriptor, myProject, null);
      if (files.length > 0) {
        final List<String> paths = new ArrayList<>();
        for (VirtualFile file : files) {
          paths.add(file.getPath());
        }
        myFile.setText(StringUtil.join(paths, File.pathSeparator));
      }
      else {
        myFile.setText("");
      }
    }, this);
  }

  @Override
  public void dispose() {
  }

  private static void collectKeyValuePairs(NameValueTableModel data, List<RestClientRequest.KeyValuePair> keyValuePairs) {
    for(int i=0; i< data.getElementsCount(); i++) {
      keyValuePairs.add(new RestClientRequest.KeyValuePair(data.getName(i), data.getValue(i)));
    }
  }

  private static void applyKeyValuePairs(NameValueTableModel data, List<RestClientRequest.KeyValuePair> keyValuePairs, JBListTable table) {
    data.clear();
    for (RestClientRequest.KeyValuePair header : keyValuePairs) {
      data.addPropertyRow(header.getKey(), header.getValue());
    }
    ((AbstractTableModel)table.getTable().getModel()).fireTableDataChanged();
  }

  public void setHeader(final String name, final String value) {
    myHeaderData.addProperty(name, value);
    ((AbstractTableModel)myHeaderTable.getTable().getModel()).fireTableDataChanged();
  }

  public void addRequestParameter(String name, String value) {
    myRequestData.addPropertyRow(name, value);
    ((AbstractTableModel)myRequestTable.getTable().getModel()).fireTableDataChanged();
  }

  private static String[] collectMimeTypes() {
    return MimeTypes.PREDEFINED_MIME_VARIANTS;
  }

  public void stopEditing() {
    myHeaderTable.stopEditing();
    myRequestTable.stopEditing();
  }

  private class NameValueListTable extends JBListTable {
    private final Project myProject;
    private final String myNameValueSeparator;
    private final Collection<String> myNames;
    private final EditorTextFieldJBTableRowRenderer myRowRenderer;

    NameValueListTable(Project project, JBTable baseTable, String nameValueSeparator, Collection<String> names, Disposable parent) {
      super(baseTable, parent);
      myProject = project;
      myNameValueSeparator = nameValueSeparator;
      myNames = names;
      myRowRenderer = new EditorTextFieldJBTableRowRenderer(myProject, parent) {
        @Override
        protected String getText(JTable table, int row) {
          String headerName = (String)myInternalTable.getValueAt(row, 0);
          String headerValue = (String)myInternalTable.getValueAt(row, 1);
          final int index = headerValue.indexOf("\n");
          if (index >= 0) {
            headerValue = headerValue.substring(0, index) + "...";
          }
          return " " + headerName + myNameValueSeparator + headerValue;
        }
      };
    }

    @Override
    protected JBTableRowRenderer getRowRenderer(int row) {
      return myRowRenderer;
    }

    @Override
    protected JBTableRowEditor getRowEditor(final int row) {
      return new JBTableRowEditor() {
        private EditorTextField myNameEditor;
        private EditorComboBox myValueEditor;

        @Override
        public void prepareEditor(final JTable table, int row) {
          String headerName = row < myInternalTable.getRowCount() ? (String)myInternalTable.getValueAt(row, 0) : "";
          String headerValue = row < myInternalTable.getRowCount() ? (String)myInternalTable.getValueAt(row, 1) : "";

          setLayout(new GridLayout(1, 2));
          myNameEditor = new TextFieldWithAutoCompletion<>(myProject,
                                                           new TextFieldWithAutoCompletion.StringsCompletionProvider(myNames, null),
                                                           myNames != null, headerName);
          myNameEditor.addDocumentListener(new RowEditorChangeListener(0));
          add(createLabeledPanel("Name:", myNameEditor));

          myValueEditor = new EditorComboBox(headerValue) {
            @Override
            protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
              if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ENTER) {
                stopEditing();
                e.consume();
                return true;
              }
              return super.processKeyBinding(ks, e, condition, pressed);
            }
          };
          if (headerName.equals(ACCEPT)) {
            myValueEditor.setHistory(myMimeTypes);
          }
          myValueEditor.prependItem(headerValue);
          myValueEditor.addDocumentListener(new RowEditorChangeListener(1));
          add(createLabeledPanel("Value:", myValueEditor));
        }

        @Override
        public JBTableRow getValue() {
          return new JBTableRow() {
            @Override
            public Object getValueAt(int column) {
              switch (column) {
                case 0: return myNameEditor.getText().trim();
                case 1: return myValueEditor.getText().trim();
              }
              return null;
            }
          };
        }

        @Override
        public JComponent getPreferredFocusedComponent() {
          String headerName = row >= myInternalTable.getRowCount() ? null : (String)myInternalTable.getValueAt(row, 0);
          return StringUtil.isEmpty(headerName) ? myNameEditor.getFocusTarget() : myValueEditor;
        }

        @Override
        public JComponent[] getFocusableComponents() {
          return new JComponent[] { myNameEditor.getFocusTarget(), myValueEditor };
        }
      };
    }
  }

  private class ToggleSendParamsAction extends ToggleActionButton {
    ToggleSendParamsAction() {
      super("Do not send parameters", AllIcons.Actions.CloseHovered);
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
      return myDoNotSendParams;
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
      setDoNotSendParams(state);
    }
  }
}
