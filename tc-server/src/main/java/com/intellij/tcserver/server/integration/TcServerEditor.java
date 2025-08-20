package com.intellij.tcserver.server.integration;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.javaee.appServers.appServerIntegrations.ApplicationServerPersistentDataEditor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.tcserver.sdk.TcServerUtil;
import com.intellij.tcserver.util.TcServerBundle;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.HoverHyperlinkLabel;
import com.intellij.ui.popup.ComponentPopupBuilderImpl;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class TcServerEditor extends ApplicationServerPersistentDataEditor<TcServerData> {
  private final static Logger MY_LOG = Logger.getInstance(TcServerEditor.class);

  private JPanel myMainPanel;
  private TextFieldWithBrowseButton myTcServerHomeField;
  private JComboBox myTcServerNameComboBox;
  private JLabel myErrorLabel;
  private JButton myCreateServerInstanceButton;
  private JLabel myServerNameUndefinedMessageLabel;
  private HoverHyperlinkLabel myHyperLinkLabel;
  private JButton myReloadInstancesButton;
  private JLabel myHttpPortLabel;
  private JLabel myJmxPortLabel;

  @Nls private String myPathErrorMessage;
  @Nls private String myActionErrorMessage;
  @Nls private String myPortErrorMessage;
  @Nls private String myLogPartErrorMessage;
  private boolean isServerInstancesListEmpty;
  private boolean areActionsEnabled = false;
  private TcServerVersion myVersion;

  public TcServerEditor() {
    //noinspection DialogTitleCapitalization
    myTcServerHomeField
      .addBrowseFolderListener(TcServerBundle.message("serverEditor.editorTitle"), TcServerBundle.message("serverEditor.editorDescription"),
                               null, FileChooserDescriptorFactory.createSingleFolderDescriptor());

    final DefaultComboBoxModel comboBoxModel = new DefaultComboBoxModel();
    myTcServerNameComboBox.setModel(comboBoxModel);
    myTcServerHomeField.addActionListener(new UpdateServersListener(false));
    //straight to JTextField
    myTcServerHomeField.getChildComponent().addKeyListener(new UpdateServersListener(false));
    myTcServerHomeField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        validateSdkPath();
      }
    });
    myErrorLabel.setIcon(UIUtil.getBalloonWarningIcon());
    myErrorLabel.setVisible(myPathErrorMessage != null || myActionErrorMessage != null);
    myCreateServerInstanceButton.addActionListener(new CreateInstanceActionListener());

    myReloadInstancesButton.addActionListener(new UpdateServersListener(true));

    myCreateServerInstanceButton.setEnabled(areActionsEnabled);
    myReloadInstancesButton.setEnabled(areActionsEnabled);

    ItemListener myInstanceNameSelectionListener = new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent event) {
        String portNotFound = TcServerBundle.message("not.found");
        String instanceName = (String)event.getItem();
        if (event.getStateChange() == ItemEvent.SELECTED) {
          myPortErrorMessage = null;
          if (!StringUtil.isEmpty(instanceName)) {
            String sdkPath = myTcServerHomeField.getText();
            try {
              Map<String, Integer> ports = TcServerVersion.getPorts(sdkPath, instanceName, myVersion);
              Function<String, @NlsSafe String> portsAccess = key -> {
                Integer port = ports.get(key);
                return String.valueOf(port);
              };
              myJmxPortLabel
                .setText(ports.get(TcServerUtil.JMX_PORT_KEY) != null ? portsAccess.apply(TcServerUtil.JMX_PORT_KEY) : portNotFound);
              myHttpPortLabel
                .setText(ports.get(TcServerUtil.HTTP_PORT_KEY) != null ? portsAccess.apply(TcServerUtil.HTTP_PORT_KEY) : portNotFound);
              String propertiesPath =
                TcServerUtil.getCatalinaPropertiesPath(getSdkPath(), (String)getServerNameComboBoxModel().getSelectedItem());
              TcServerData.validatePorts(ports.get(TcServerUtil.JMX_PORT_KEY), ports.get(TcServerUtil.HTTP_PORT_KEY), propertiesPath);
            }
            catch (RuntimeConfigurationException e) {
              myPortErrorMessage = e.getMessage();
            }
          }
          else {
            myJmxPortLabel.setText(portNotFound);
            myHttpPortLabel.setText(portNotFound);
          }
          updateNotifications();
        }
      }
    };
    myTcServerNameComboBox.addItemListener(myInstanceNameSelectionListener);
  }

  private void createUIComponents() {
    myHyperLinkLabel = new HoverHyperlinkLabel(null);
    final JTextArea textArea = new JTextArea();
    textArea.setEditable(false);

    final ComponentPopupBuilder builder = new ComponentPopupBuilderImpl(textArea, textArea);
    builder.setCancelOnClickOutside(true);
    builder.setMovable(true);
    builder.setCancelKeyEnabled(true);
    builder.setCancelOnOtherWindowOpen(true);
    builder.setTitle(TcServerBundle.message("serverInstanceCreatorDialog.logMessageTitle"));

    myHyperLinkLabel.addHyperlinkListener(new HyperlinkListener() {

      @Override
      public void hyperlinkUpdate(HyperlinkEvent e) {
        textArea.setText(myLogPartErrorMessage);
        DialogWrapper dialogWrapper = new LogMessageDialogWrapper(textArea, myHyperLinkLabel);
        dialogWrapper.show();
      }
    });

    myHyperLinkLabel.setVisible(false);
  }

  public static class LogMessageDialogWrapper extends DialogWrapper {
    private final JComponent myChildComponent;

    public LogMessageDialogWrapper(JTextArea textArea, JComponent parentComponent) {
      super(parentComponent, false);
      setOKButtonText(TcServerBundle.message("serverEditor.closeButtonText"));
      JScrollPane scrollPane = new JScrollPane(textArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                               ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
      textArea.setBackground(Color.WHITE);
      myChildComponent = scrollPane;
      setTitle(TcServerBundle.message("serverInstanceCreatorDialog.logMessageTitle"));
      init();
    }

    @Override
    protected JComponent createCenterPanel() {
      return myChildComponent;
    }

    @Override
    protected Action @NotNull [] createActions() {
      if (getHelpId() == null) {
        return new Action[]{getOKAction()};
      }
      else {
        return new Action[]{getOKAction(), getHelpAction()};
      }
    }
  }

  private final class UpdateServersListener extends KeyAdapter implements ActionListener {
    private final boolean myForceUpdate;
    private String myPreviousPath = "";

    private UpdateServersListener(boolean forceUpdate) {
      myForceUpdate = forceUpdate;
    }

    @Override
    public void keyReleased(KeyEvent e) {
      checkAndUpdateServers();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      checkAndUpdateServers();
    }

    private void checkAndUpdateServers() {
      String path = myTcServerHomeField.getText();
      if (myForceUpdate || !new File(path).equals(new File(myPreviousPath))) {
        ProgressManager.getInstance().runProcessWithProgressSynchronously(new ServerNamesGetter(), TcServerBundle.message(
          "serverEditor.loadInstancesProgressBarMessage"), true, null);
        myPreviousPath = path;
      }
    }
  }

  public boolean containsServerName(@NlsSafe String serverName) {
    DefaultComboBoxModel model = getServerNameComboBoxModel();
    return model.getIndexOf(serverName) != -1;
  }

  public String getSdkPath() {
    return myTcServerHomeField.getText();
  }

  public TcServerVersion getVersion() {
    return myVersion;
  }

  protected void getAndUpdateAvailableServerNames() {
    String sdkPath = myTcServerHomeField.getText();
    List<String> servers = null;
    myPathErrorMessage = null;
    myActionErrorMessage = null;
    myLogPartErrorMessage = null;
    myVersion = null;
    areActionsEnabled = true;

    try {
      myVersion = TcServerVersion.getVersion(sdkPath);
      servers = myVersion.getServers(sdkPath);
      isServerInstancesListEmpty = servers.isEmpty();
      try {
        TcServerData.validateTcServerData(sdkPath, servers.isEmpty() ? null : servers.get(servers.size() - 1), servers);
      }
      catch (RuntimeConfigurationException e) {
        //in case everything works well, but there are no instances
        myPathErrorMessage = e.getMessage();
      }
    }
    catch (RuntimeConfigurationException e) {
      myActionErrorMessage = e.getMessage();
      areActionsEnabled = false;
      isServerInstancesListEmpty = true;
    }
    catch (ExecutionException e) {
      myActionErrorMessage = TcServerBundle.message("serverEditor.failedToGetServerNames");
      myLogPartErrorMessage = e.getMessage();
      isServerInstancesListEmpty = true;
    }

    try {
      EventQueue.invokeAndWait(new ServerNameComboBoxUpdater(servers));
    }
    catch (InterruptedException | InvocationTargetException e) {
      MY_LOG.warn("Failed to update error message.", e);
    }
  }

  private class ServerNamesGetter implements Runnable {
    @Override
    public void run() {
      getAndUpdateAvailableServerNames();
    }
  }

  private class ServerNameComboBoxUpdater implements Runnable {
    private final List<@NlsSafe String> myServers;

    ServerNameComboBoxUpdater(List<String> servers) {
      myServers = servers;
    }

    //just data application to ui

    @Override
    public void run() {
      if (myActionErrorMessage == null) {
        //if we are here, then everything is valid, and there is at least one server
        DefaultComboBoxModel model = getServerNameComboBoxModel();
        model.removeAllElements();
        for (@NlsSafe String myServer : myServers) {
          model.addElement(myServer);
        }
      }

      updateNotifications();
    }
  }

  private void updateNotifications() {
    //error labels
    if (myPathErrorMessage != null || myActionErrorMessage != null || myPortErrorMessage != null) {
      String message = myPathErrorMessage;
      if (message == null) {
        message = myActionErrorMessage;
      }
      if (message == null) {
        message = myPortErrorMessage;
      }
      myErrorLabel.setText(message);
      myErrorLabel.setVisible(true);
    }
    else {
      myErrorLabel.setVisible(false);
    }

    if (!StringUtil.isEmpty(myLogPartErrorMessage)) {
      myHyperLinkLabel.setVisible(true);
    }
    else {
      myHyperLinkLabel.setVisible(false);
    }

    //enable actions
    myCreateServerInstanceButton.setEnabled(areActionsEnabled);
    myReloadInstancesButton.setEnabled(areActionsEnabled);

    //improve server presentation
    if (isServerInstancesListEmpty) {
      myTcServerNameComboBox.setVisible(false);
      getServerNameComboBoxModel().removeAllElements();
      myServerNameUndefinedMessageLabel.setVisible(true);
    }
    else {
      myTcServerNameComboBox.setVisible(true);
      myServerNameUndefinedMessageLabel.setVisible(false);
    }
  }

  private DefaultComboBoxModel getServerNameComboBoxModel() {
    return (DefaultComboBoxModel)myTcServerNameComboBox.getModel();
  }


  private void validateSdkPath() {
    try {
      String sdkPath = TcServerUtil.validateSdkPath(myTcServerHomeField.getText());
      myPathErrorMessage = null;
      areActionsEnabled = true;
    }
    catch (RuntimeConfigurationException ex) {
      //the problem is really in path
      areActionsEnabled = false;
    }
    updateNotifications();
  }

  @Override
  protected void resetEditorFrom(@NotNull TcServerData s) {
    //apply
    myTcServerHomeField.setText(FileUtil.toSystemDependentName(s.getSdkPath()));

    //must be before adding servers to let them get their versions
    myVersion = s.getVersion();

    DefaultComboBoxModel model = (DefaultComboBoxModel)myTcServerNameComboBox.getModel();
    model.removeAllElements();
    for (@NlsSafe String serverName : s.getAvailableServers()) {
      model.addElement(serverName);
    }
    myTcServerNameComboBox.getModel().setSelectedItem(s.getServerName());

    //read through TcServerUril.getPorts after server instance name renewal
    //  myJmxPortLabel.setText(s.getJmxPort() != null ? s.getJmxPort().toString() : "");
    //  myHttpPortLabel.setText(s.getHttpPort() != null ? s.getHttpPort().toString() : "");


    //reset data
    myPathErrorMessage = null;
    myActionErrorMessage = null;
    myLogPartErrorMessage = null;
    myPortErrorMessage = null;
    areActionsEnabled = true;
    isServerInstancesListEmpty = s.getAvailableServers().isEmpty();

    //validate
    try {
      TcServerData
        .validateTcServerData(myTcServerHomeField.getText(), (String)model.getSelectedItem(), s.getAvailableServers());
    }
    catch (RuntimeConfigurationException e) {
      myPathErrorMessage = e.getMessage();
      validateSdkPath();
    }
    //port validation already made when   myTcServerNameComboBox.getModel().setSelectedItem(s.getServerName());
    updateNotifications();
  }

  @Override
  protected void applyEditorTo(@NotNull TcServerData s) {
    String sdkPath = FileUtil.toSystemIndependentName(myTcServerHomeField.getText());
    ComboBoxModel model = myTcServerNameComboBox.getModel();
    String selectedName = (String)model.getSelectedItem();
    List<String> availableServers = new ArrayList<>(model.getSize());
    for (int i = 0; i < model.getSize(); i++) {
      availableServers.add((String)model.getElementAt(i));
    }

    s.setSdkPath(sdkPath);
    s.setServerName(selectedName == null ? "" : selectedName);
    s.setAvailableServers(availableServers);

    try {
      s.setJmxPort(Integer.valueOf(myJmxPortLabel.getText()));
    }
    catch (NumberFormatException e) {
      s.setJmxPort(null);
    }

    try {
      s.setHttpPort(Integer.valueOf(myHttpPortLabel.getText()));
    }
    catch (NumberFormatException e) {
      s.setHttpPort(null);
    }

    s.setVersion(myVersion);
  }

  @Override
  @NotNull
  protected JComponent createEditor() {
    return myMainPanel;
  }

  private class CreateInstanceActionListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      try {
        ServerInstanceCreatorDialog dialog = new ServerInstanceCreatorDialog(TcServerEditor.this);
        if (dialog.showAndGet()) {
          String instanceName = dialog.getInstanceName();
          String templatePath = dialog.getTemplatePath(myVersion);
          ProgressManager.getInstance()
            .runProcessWithProgressSynchronously(new ServerInstanceCreator(instanceName, templatePath), TcServerBundle.message(
              "serverInstanceCreatorDialog.progressBarMessage", instanceName), true, null);
        }
      }
      catch (RuntimeConfigurationException exception) {
        myPathErrorMessage = exception.getMessage();
        myLogPartErrorMessage = null;
        updateNotifications();
      }
    }
  }

  private final class ServerInstanceCreator implements Runnable {
    @NlsSafe private final String myServerName;
    @NlsSafe private final String myTemplatePath;

    private ServerInstanceCreator(String serverName, String templatePath) {
      myServerName = serverName;
      if (StringUtil.isEmpty(templatePath)) {
        myTemplatePath = null;
      }
      else {
        myTemplatePath = templatePath;
      }
    }

    @Override
    public void run() {
      myActionErrorMessage = null;
      myLogPartErrorMessage = null;

      boolean success = true;
      try {
        myVersion.createServer(getSdkPath(), myServerName, myTemplatePath, TcServerEditor.this.containsServerName(myServerName));
        getAndUpdateAvailableServerNames();
      }
      catch (RuntimeConfigurationException e) {
        myActionErrorMessage = e.getMessage();
        success = false;
      }
      catch (ExecutionException e) {
        myActionErrorMessage = TcServerBundle.message("serverEditor.failedToCreateServer", myServerName);
        myLogPartErrorMessage = e.getMessage();
        success = false;
      }

      final boolean creationSucceeded = success;
      try {
        EventQueue.invokeAndWait(() -> {
          if (creationSucceeded) {
            //everything else is done after server instances list refresh
            myTcServerNameComboBox.getModel().setSelectedItem(myServerName);
          }
          else {
            updateNotifications();
          }
        });
      }
      catch (InterruptedException | InvocationTargetException e) {
        MY_LOG.warn("Failed to update error message.", e);
      }
    }
  }
}
