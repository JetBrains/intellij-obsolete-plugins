package com.intellij.restClient;

import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.codeInsight.daemon.impl.analysis.FileHighlightingSetting;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightingSettingsPerFile;
import com.intellij.execution.ui.layout.impl.JBRunnerTabs;
import com.intellij.httpClient.execution.common.CommonClientBodyFileHint;
import com.intellij.httpClient.execution.common.CommonClientRequest;
import com.intellij.httpClient.execution.common.CommonClientResponse;
import com.intellij.httpClient.execution.common.CommonClientResponseBody;
import com.intellij.httpClient.http.request.HttpRequestPsiConverter;
import com.intellij.httpClient.http.request.HttpRequestPsiFactory;
import com.intellij.httpClient.http.request.HttpRequestVariableSubstitutor;
import com.intellij.httpClient.http.request.psi.HttpRequest;
import com.intellij.httpClient.http.request.run.HttpRunRequestInfo;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileChooser.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.Segment;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.*;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.util.net.HttpConfigurable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.httpClient.execution.*;
import net.miginfocom.swing.MigLayout;
import org.apache.http.cookie.Cookie;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;

import static com.intellij.util.ObjectUtils.doIfNotNull;

/**
 * @author Konstantin Bulenkov
 */
@SuppressWarnings("UnstableApiUsage")
public final class RESTClient implements RestClientResponseListener, Disposable {
  private final Project myProject;
  private TextFieldWithHistory myHttpMethod;
  private ComboBox myURL;
  private VirtualFile myResponseFile;
  private EditorEx myResponse;
  private FileType mySelectedResponseFileType = PlainTextFileType.INSTANCE;
  private final Ref<FileType> myFileTypeFromResponse = new Ref<>(PlainTextFileType.INSTANCE);
  private final Set<FileType> myBuiltinFileTypes = new HashSet<>();
  private JPanel myMainPanel;
  private final EditorTextField myHeader;
  private JTextField myURLBase;
  private final JLabel myStatus;
  private final JPanel myResponseTab;
  private JPanel myMainToolbarPlaceholder;
  private JPanel myRequestPanel;
  private JPanel myTabsPlaceholder;
  private JTextField myURLTextField;
  private final RestClientController myController;
  private final JPanel myWrappedMainPanel = new RestHelpWrapper();
  private final RestClientRequestPanel myClientRequestPanel;
  private boolean myHaveURLTemplates;
  private final RESTCookiesPanel myCookiesPanel;

  private final JBTabs myTabs;
  private final DumbAwareAction myReformatAction;

  public RESTClient(final Project project) {
    myProject = project;
    myController = new RestClientControllerImpl(project, null, null, false) {
      @Override
      protected void addToHistory(@NotNull Project project, @NotNull CommonClientRequest request, @Nullable CommonClientResponse response) {
        RestClientSettings.getInstance(project).addToHistory((RestClientRequest) request);
      }
    };
    myResponseTab = new NonOpaquePanel(new BorderLayout());
    myClientRequestPanel = new RestClientRequestPanel(project, this);
    myCookiesPanel = new RESTCookiesPanel(project, this);

    myReformatAction = new ReformatAction();

    layoutMainComponents();
    initUIComponents();

    myRequestPanel.setBorder(JBUI.Borders.emptyTop(2));
    myWrappedMainPanel.add(myMainPanel);

    myHeader = new MyEditorTextField(myProject, true);

    myTabs = new JBRunnerTabs(project, project);
    myTabs.getPresentation().setInnerInsets(JBUI.emptyInsets()).setPaintBorder(0, 0, 0, 0).setPaintFocus(false)
      .setRequestFocusOnLastFocusedComponent(true);
    myTabs.getComponent().setBorder(JBUI.Borders.emptyLeft(1));
    myTabs.addTab(new TabInfo(myClientRequestPanel).setText("Request"));
    myTabs.addTab(new TabInfo(myCookiesPanel).setText("Cookies"));
    myTabs.addTab(new TabInfo(myResponseTab).setText("Response"));
    myTabs.addTab(new TabInfo(myHeader).setText("Response Headers"));
    myTabsPlaceholder.add(myTabs.getComponent(), BorderLayout.CENTER);

    createMainToolbar();
    createResponseToolbar();

    myStatus = new JLabel();
    UIUtil.applyStyle(UIUtil.ComponentStyle.SMALL, myStatus);
    myResponseTab.add(BorderLayout.SOUTH, myStatus);
    myHttpMethod.addActionListener(event -> updateBodyEnabled());

    List<RestClientRequest> history = RestClientSettings.getInstance(project).REQUEST_HISTORY;
    if (history.size() > 0) {
      loadRequestFromHistory(history.get(0));
    }

    updateBodyEnabled();
    myClientRequestPanel.updateFileEnabled();
  }

  private void layoutMainComponents() {
    myMainPanel = new JPanel(new MigLayout("fill, ins 0, gap 0"));

    myURL = new ComboBox();

    myURLBase = new JBTextField("http://localhost:8080/resources");
    myURLTextField = new JBTextField();
    myHttpMethod = createHttpMethod();

    myTabsPlaceholder = new JPanel(new BorderLayout());
    myMainToolbarPlaceholder = new JPanel(new BorderLayout());
    myRequestPanel = new JPanel(new MigLayout("fillx, ins 0 0 3 0", "[min!][][grow]"));

    myMainPanel.add(myTabsPlaceholder, "grow");
    myMainPanel.add(myMainToolbarPlaceholder, "dock west, w pref!");

    final JPanel deprecatedPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    myMainPanel.add(deprecatedPanel,  "dock north");
    myMainPanel.add(myRequestPanel,  "dock north");

    myRequestPanel.add(new JLabel("HTTP method:"));
    myRequestPanel.add(myHttpMethod);

    JPanel hostPane = new JPanel(new MigLayout("ins 0 0 0 3, fillx", "[min!][grow]"));
    JPanel pathPane = new JPanel(new MigLayout("ins 0 3 0 0, fillx", "[min!][grow]"));

    JPanel urlPane = new JPanel(new MigLayout("fillx, ins 0, hidemode 3"));
    urlPane.add(myURLTextField, "growx");
    urlPane.add(myURL, "growx");

    hostPane.add(new JLabel("Host/port:"));
    hostPane.add(myURLBase, "wmin 100, growx");
    pathPane.add(new JLabel("Path:"));
    pathPane.add(urlPane, "wmin 100, growx");

    OnePixelSplitter onePixelSplitter = new OnePixelSplitter(false, 1f/4);
    onePixelSplitter.setFirstComponent(hostPane);
    onePixelSplitter.setSecondComponent(pathPane);

    myRequestPanel.add(onePixelSplitter, "grow");
  }

  private void updateBodyEnabled() {
    String item = (String)myHttpMethod.getSelectedItem();
    myClientRequestPanel.setCanHasBody("POST".equals(item) || "PUT".equals(item) || "PATCH".equals(item));
  }

  private void createMainToolbar() {
    DefaultActionGroup group = new DefaultActionGroup();
    AnAction submitRequestAction = new DumbAwareAction(RestClientLegacyBundle.messagePointer("action.DumbAware.RESTClient.text.submit.request"),
                                                       () -> "", AllIcons.Actions.Execute) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        onGoToUrlAction();
      }
    };
    submitRequestAction.registerCustomShortcutSet(CommonShortcuts.CTRL_ENTER, myMainPanel);
    group.add(submitRequestAction);
    for (RestClientCustomActionsProvider provider : RestClientCustomActionsProvider.EP_NAME.getExtensionList()) {
      group.addAll(provider.getCustomActions(this));
    }
    group.add(new DumbAwareAction(RestClientLegacyBundle.messagePointer("action.DumbAware.RESTClient.text.update.resource.paths.from.code"),
                                  () -> "", AllIcons.Actions.Refresh) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        updateResourcePaths();
      }

      @Override
      public void update(@NotNull AnActionEvent e) {
      }
    });
    RequestHistoryAction historyAction = new RequestHistoryAction();
    historyAction.registerCustomShortcutSet(CommonShortcuts.getRecentFiles(), myMainPanel);
    group.add(historyAction);
    group.add(new ExportRequestAction());
    group.add(new ImportRequestAction());
    group.add(new GenerateAuthAction());
    group.add(new ConfigureProxyAction());
    group.add(
      new DumbAwareAction(RestClientLegacyBundle.messagePointer("action.DumbAware.RESTClient.text.close"), () -> null, AllIcons.Actions.Cancel) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        ToolWindowManager.getInstance(myProject).unregisterToolWindow(CreateRestClientAction.getRestClient());
        Disposer.dispose(RESTClient.this);
      }
    });
    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("RestClient", group, false);
    myMainToolbarPlaceholder.add(toolbar.getComponent(), BorderLayout.WEST);
  }

  private void updateResourcePaths() {
  }

  private void createResponseToolbar() {
    DefaultActionGroup group = new DefaultActionGroup();
    group.add(myReformatAction);
    group.add(new OpenInBrowserAction());
    group.add(new HighlightAction("View as text", "PLAIN_TEXT", AllIcons.FileTypes.Text));
    group.add(new HighlightAction("View as HTML", "HTML", AllIcons.FileTypes.Html));
    group.add(new HighlightAction("View as XML", "XML", AllIcons.FileTypes.Xml));
    group.add(new HighlightAction("View as JSON", "JSON", AllIcons.FileTypes.Json));
    group.add(new HighlightAction("View according to MIME type", myFileTypeFromResponse, AllIcons.FileTypes.Custom));
    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("RestClientResponseToolbar", group, false);
    myResponseTab.add(toolbar.getComponent(), BorderLayout.WEST);
  }

  private final class ConfigureProxyAction extends DumbAwareAction {
    private ConfigureProxyAction() {
      super("Configure HTTP Proxy", "Show dialog to configure the HTTP proxy settings", AllIcons.General.Settings);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      HttpConfigurable.editConfigurable(myMainPanel);
    }
  }

  private class HighlightAction extends ToggleAction implements DumbAware {
    private final Ref<FileType> myFileType;

    HighlightAction(String text, @NotNull String fileType, Icon icon) {
      this(text, Ref.create(FileTypeManager.getInstance().findFileTypeByName(fileType)), icon);
    }

    HighlightAction(String text, @NotNull Ref<FileType> fileType, Icon icon) {
      super(text, null, icon);
      myFileType = fileType;
      if (!isMimeTypeRef(fileType)) {
        myBuiltinFileTypes.add(fileType.get());
      }
    }

    private FileType getFileType() {
      return myFileType.get();
    }

    private boolean isMimeTypeRef(Ref<FileType> fileType) {
      return fileType == myFileTypeFromResponse;
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
      return mySelectedResponseFileType == getFileType();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
      if (!state) return;
      RestClientFileUtil.deleteFile(myResponseFile);
      FileType fileType = getFileType();
      myResponseFile = RestClientFileUtil.createFile(myResponse.getDocument().getText(), fileType);
      updateEditor(FileDocumentManager.getInstance().getDocument(myResponseFile), fileType);
      mySelectedResponseFileType = fileType;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      e.getPresentation().setEnabled(e.getProject() != null && myResponse.getDocument().getTextLength() > 0);
      if (isMimeTypeRef(myFileType)) {
        e.getPresentation().setVisible(!myBuiltinFileTypes.contains(getFileType()));
      }
      super.update(e);
    }
  }

  private final class OpenInBrowserAction extends DumbAwareAction {
    private OpenInBrowserAction() {
      super("Open in Browser", "Open response content in browser", AllIcons.Nodes.PpWeb);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      myController.openResponseInBrowser(getResponse());
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      e.getPresentation().setEnabled(e.getProject() != null && myResponse.getDocument().getTextLength() > 0);
    }
  }

  @NotNull
  private RestClientRequest createRequest() {
    RestClientRequest request = getRequest();
    myClientRequestPanel.saveToRequest(request);
    myCookiesPanel.saveToRequest(request);
    return request;
  }

  public String getRequestURL() {
    return getRequest().getURL();
  }
  @NotNull
  private RestClientRequest getRequest() {
    RestClientRequest request = new RestClientRequest();
    request.httpMethod = getHttpMethod();
    request.urlBase = myURLBase.getText();
    request.urlPath = getURL();
    return request;
  }

  private String getURL() {
    if (myHaveURLTemplates) {
      return (String)myURL.getEditor().getItem();
    }
    return myURLTextField.getText();
  }

  private void loadRequestFromHistory(RestClientRequest request) {
    myHttpMethod.setSelectedItem(request.httpMethod);
    myURLBase.setText(request.urlBase);
    setURL(request.urlPath);
    myClientRequestPanel.loadFromRequest(request);
    myCookiesPanel.loadFromRequest(request);
    myTabs.select(myTabs.getTabAt(0), false);
  }

  private void setURL(String path) {
    if (myHaveURLTemplates) {
      myURL.getEditor().setItem(path);
    }
    else {
      this.myURLTextField.setText(path);
    }
  }

  public void setURLTemplates(final Set<String> templates) {
    myHaveURLTemplates = !templates.isEmpty();
    myURL.setVisible(myHaveURLTemplates);
    myURLTextField.setVisible(!myHaveURLTemplates);

    if (!templates.isEmpty()) {
      myURL.removeAllItems();
      for (String template : templates) {
        myURL.addItem(template);
      }
      myURL.addItem("/application.wadl");
    }
  }

  private String getResponse() {
    return myResponse.getDocument().getText();
  }

  private void updateEditor(final Document document, FileType fileType) {
    if (myResponse != null) {
      myReformatAction.unregisterCustomShortcutSet(myResponse.getComponent());
      myResponseTab.remove(myResponse.getComponent());
      EditorFactory.getInstance().releaseEditor(myResponse);
    }
    myResponse = (EditorEx)EditorFactory.getInstance().createEditor(document, myProject, fileType, true);
    disableHighlighting(document);
    myResponse.getSettings().setUseSoftWraps(true);
    myResponse.getSettings().setLineMarkerAreaShown(false);
    myResponse.getSettings().setLineNumbersShown(false);
    myReformatAction.registerCustomShortcutSet(ActionManager.getInstance().getAction("ReformatCode").getShortcutSet(),
      myResponse.getComponent(), this);
    myResponseTab.add(myResponse.getComponent(), BorderLayout.CENTER);
    myResponseTab.validate();
  }

  private void disableHighlighting(Document document) {
    final PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(document);
    if (psiFile != null) {
      HighlightingSettingsPerFile.getInstance(myProject).setHighlightingSettingForRoot(psiFile, FileHighlightingSetting.SKIP_INSPECTION);
    }
  }

  public String getHttpMethod() {
    return (String)myHttpMethod.getSelectedItem();
  }

  private TextFieldWithHistory createHttpMethod() {
    TextFieldWithHistory textFieldWithHistory = new TextFieldWithStoredHistory("RESTClient.httpMethod");
    textFieldWithHistory.setHistorySize(15);
    final List<String> newHistory = new ArrayList<>(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"));
    for (String method : textFieldWithHistory.getHistory()) {
      if (!newHistory.contains(method)) {
        newHistory.add(method);
      }
    }
    textFieldWithHistory.setHistory(newHistory);
    textFieldWithHistory.setSelectedIndex(0);

    return textFieldWithHistory;
  }

  private void initUIComponents() {
    myURLBase.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(final KeyEvent e) {
        super.keyReleased(e);
        myURLBase.setBackground(myController.isValidURL(myURLBase.getText()) ? UIUtil.getTextFieldBackground()
          : new JBColor(JBColor.PINK, new Color(75, 34, 27)));
      }
    });

    final KeyAdapter enterListener = new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        super.keyReleased(e);
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          onGoToUrlAction();
        }
      }
    };

    myURLBase.addKeyListener(enterListener);
    myURL.getEditor().getEditorComponent().addKeyListener(enterListener);
    myURLTextField.addKeyListener(enterListener);

    updateResourcePaths();

    myURL.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        String s = (String)myURL.getSelectedItem();
        if (s == null) return;
        if (s.contains("{") && s.contains("}")) {
          int start = s.indexOf("{");
          int end = s.indexOf("}");
          if (start < end) {
            ((JTextField)myURL.getEditor().getEditorComponent()).setSelectionStart(start);
            ((JTextField)myURL.getEditor().getEditorComponent()).setSelectionEnd(end + 1);
          }
        }
      }
    });

    updateEditor(EditorFactory.getInstance().createDocument(""), mySelectedResponseFileType);
  }


  public void onGoToUrlAction(RestClientRequestProcessor @NotNull ... processors) {
    myClientRequestPanel.stopEditing();
    splitParameters();
    myTabs.select(myTabs.getTabAt(2), false);
    IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager
      .getGlobalInstance().requestFocus(myResponse.getContentComponent(), true));
    FileDocumentManager.getInstance().saveAllDocuments();
    myController.onGoButtonClick(getHttpRunRequestInfo(), true, this, processors);
    myHttpMethod.addCurrentTextToHistory();
  }

  @NotNull
  private HttpRunRequestInfo getHttpRunRequestInfo() {
    var request = createRequest();
    var requestPsiFile = HttpRequestPsiFactory
            .createDummyFile(myProject, HttpRequestPsiConverter.toPsiHttpRequest(request, null, true));
    var httpRequest = PsiTreeUtil.findChildOfType(requestPsiFile, HttpRequest.class, false);
    return HttpRunRequestInfo.create(
            Objects.requireNonNull(httpRequest),
            new SmartPsiElementPointer<>() {
              @Override
              public @NotNull HttpRequest getElement() {
                return httpRequest;
              }

              @Override
              public @NotNull PsiFile getContainingFile() {
                return requestPsiFile;
              }

              @Override
              public @NotNull Project getProject() {
                return myProject;
              }

              @Override
              public VirtualFile getVirtualFile() {
                return null;
              }

              @Override
              public @Nullable Segment getRange() {
                return null;
              }

              @Override
              public @Nullable Segment getPsiRange() {
                return null;
              }
            },
            HttpRequestVariableSubstitutor.empty()
    );
  }

  private void splitParameters() {
    URI uri;
    try {
      uri = new URI(myURLBase.getText());
    }
    catch (URISyntaxException ex) {
      return;
    }
    String path = uri.getPath();
    if (!StringUtil.isEmpty(path)) {
      if (!"/".equals(path) || StringUtil.isEmpty(getURL())) {
        setURL(path);
      }
      String query = uri.getQuery();
      if (query != null) {
        addParameters(query);
      }
      try {
        URI uriWithoutPath = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), null, null, null);
        myURLBase.setText(uriWithoutPath.toString());
      }
      catch (URISyntaxException e) {
        // ignore
      }
    }
    else {
      path = getURL();
      int queryStart = path.indexOf('?');
      if (queryStart > 0) {
        addParameters(path.substring(queryStart+1));
        setURL(path.substring(0, queryStart));
      }
    }
  }

  private void addParameters(String query) {
    for (String s : StringUtil.split(query, "&")) {
      int pos = s.indexOf('=');
      if (pos > 0) {
        myClientRequestPanel.addRequestParameter(s.substring(0, pos), s.substring(pos + 1));
      }
    }
  }

  public JComponent getComponent() {
    return myWrappedMainPanel;
  }

  @Override
  public void dispose() {
    if (myResponse != null && !myResponse.isDisposed()) {
      EditorFactory.getInstance().releaseEditor(myResponse);
    }
    RestClientFileUtil.deleteFile(myResponseFile);
  }

  @Override
  public void onStart() {
    onResponse(null, "", "text/plain", "");
  }

  @Override
  public void onErrorResponse(String response) {
    onResponse(null, response, "text/plain", "");
  }

  @Override
  public void onResponse(@Nullable String header, @NotNull CommonClientResponseBody body, @NotNull String status, long executionTime) {
    myHeader.setText(header == null ? "" : header);


    String responseText;
    FileType fileType;
    if (!(body instanceof CommonClientResponseBody.Text)) {
      responseText = RestClientLegacyBundle.message("rest.client.unsupported.response.message");
      fileType = PlainTextFileType.INSTANCE;
    } else {
      responseText = ((CommonClientResponseBody.Text) body).getContent();
      fileType = doIfNotNull(body.getFileHint(), CommonClientBodyFileHint::getFileTypeHint);
      if (fileType == null) {
        fileType = PlainTextFileType.INSTANCE;
      }
    }

    RestClientFileUtil.deleteFile(myResponseFile);
    myResponseFile = RestClientFileUtil.createFile(responseText, fileType);
    updateEditor(FileDocumentManager.getInstance().getDocument(myResponseFile), fileType);
    mySelectedResponseFileType = fileType;
    myFileTypeFromResponse.set(fileType);
    myStatus.setText(status);
  }

  @Override
  public void onSetCookies(Collection<Cookie> cookies) {
    myCookiesPanel.setCookies(cookies);
  }

  public void importRequest(VirtualFile file) {
    try {
      loadRequestFromHistory(RestClientSerializer.loadFromFile(file));
    }
    catch (RestClientSerializer.RestClientSerializationException e) {
      Messages.showErrorDialog(myProject, e.getMessage(), "Error Loading Request");
    }
  }

  private static class RestHelpWrapper extends JPanel implements DataProvider {
    RestHelpWrapper() {
      super(new BorderLayout());
    }

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
      if (PlatformDataKeys.HELP_ID.is(dataId)) {
        return "reference.tool.windows.rest.client";
      }
      return null;
    }
  }

  private final class GenerateAuthAction extends DumbAwareAction {
    private GenerateAuthAction() {
      super("Generate 'Authorization' Header", "Generates a header for HTTP basic authorization from a username and a password",
        AllIcons.Nodes.SecurityRole);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      UsernamePasswordForm form = new UsernamePasswordForm(myMainPanel);
      if (form.showAndGet()) {
        String decoded = form.getUsername() + ":" + form.getPassword();
        String encoded = Base64.getEncoder().encodeToString(decoded.getBytes(StandardCharsets.UTF_8));
        myClientRequestPanel.setHeader("Authorization", "Basic " + encoded);
      }
    }
  }

  private final class RequestHistoryAction extends DumbAwareAction {
    private RequestHistoryAction() {
      super("Replay Recent Requests", "Show and replay recently executed requests", AllIcons.Actions.Back);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      List<RestClientRequest> history = RestClientSettings.getInstance(myProject).REQUEST_HISTORY;
      BaseListPopupStep<RestClientRequest> step = new BaseListPopupStep<>("Recent Requests", history) {
        @Override
        public PopupStep onChosen(RestClientRequest selectedValue, boolean finalChoice) {
          loadRequestFromHistory(selectedValue);
          return FINAL_CHOICE;
        }
      };
      ListPopup popup = JBPopupFactory.getInstance().createListPopup(step);
      InputEvent event = e.getInputEvent();
      Component c = event != null ? event.getComponent() : null;
      if (c != null) {
        popup.showUnderneathOf(c);
      }
      else {
        popup.showInBestPositionFor(e.getDataContext());
      }
    }
    @Override
    public void update(@NotNull AnActionEvent e) {
      e.getPresentation().setEnabled(RestClientSettings.getInstance(myProject).REQUEST_HISTORY.size() > 0);
    }
  }

  private final class ExportRequestAction extends DumbAwareAction {
    private ExportRequestAction() {
      super("Export Request", "Save current request to .xml file", AllIcons.ToolbarDecorator.Export);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      FileSaverDialog dialog = FileChooserFactory.getInstance()
        .createSaveFileDialog(new FileSaverDescriptor("Save Request as XML", "", "xml"), getEventProject(e));
      VirtualFileWrapper file = dialog.save(null);
      if (file != null) {
        try {
          RestClientSerializer.saveToFile(createRequest(), file.getFile());
        }
        catch (RestClientSerializer.RestClientSerializationException e1) {
          Messages.showErrorDialog(myProject, e1.getMessage(), "Export Request Failed");
        }
      }
    }
  }

  private final class ImportRequestAction extends DumbAwareAction {
    private ImportRequestAction() {
      super("Import Request", "Load request from an .xml file", AllIcons.ToolbarDecorator.Import);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      FileChooserDescriptor descriptor = new FileTypeDescriptor("Select .xml File with REST Client Request", "xml");
      FileChooser.chooseFiles(descriptor, null, myMainPanel, myProject.getBaseDir(), new FileChooser.FileChooserConsumer() {
        @Override
        public void consume(List<VirtualFile> files) {
          if (files.size() > 0) {
            importRequest(files.get(0));
          }
        }

        @Override
        public void cancelled() {}
      });
    }
  }

  static class MyEditorTextField extends EditorTextField {
    MyEditorTextField(Project project, final boolean viewer) {
      super(EditorFactory.getInstance().createDocument(""), project, PlainTextFileType.INSTANCE, viewer, false);
    }

    @Override
    protected EditorEx createEditor() {
      EditorEx editor = super.createEditor();
      editor.getSettings().setUseSoftWraps(true);
      editor.setVerticalScrollbarVisible(true);
      return editor;
    }
  }

  private class ReformatAction extends DumbAwareAction {
    ReformatAction() {
      super("Reformat response", null, AllIcons.Actions.ToggleSoftWrap);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      final PsiFile file = PsiManager.getInstance(myProject).findFile(myResponseFile);
      new ReformatCodeProcessor(myProject, new PsiFile[]{file}, "Reformat response", null, false).run();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      e.getPresentation().setEnabled(e.getProject() != null && myResponse.getDocument().getTextLength() > 0);
    }
  }
}