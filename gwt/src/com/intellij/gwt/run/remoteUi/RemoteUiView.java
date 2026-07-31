package com.intellij.gwt.run.remoteUi;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.RunContentBuilder;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.execution.ui.layout.PlaceInGrid;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.run.remoteUi.responses.FailureResponse;
import com.intellij.gwt.run.remoteUi.responses.InitializeResponse;
import com.intellij.gwt.run.remoteUi.responses.RemoteUiResponse;
import com.intellij.icons.AllIcons;
import com.intellij.ide.browsers.OpenUrlHyperlinkInfo;
import com.intellij.ide.browsers.WebBrowser;
import com.intellij.javaee.appServers.run.execution.ConsoleViewWrapper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.content.Content;
import com.intellij.util.io.socketConnection.AbstractResponseHandler;
import com.intellij.util.io.socketConnection.SocketConnection;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class RemoteUiView extends ConsoleViewWrapper {
  private final ConsoleView myServerConsole;
  private final ModuleLogComponent myModuleLogComponent;
  private final RemoteUiConnection myUiConnection;
  private final boolean myDebugSession;

  public RemoteUiView(Project project, final ConsoleView delegate, RemoteUiConnection uiConnection, boolean isDebugSession) {
    super(delegate);
    Disposer.register(this, delegate);
    myUiConnection = uiConnection;
    myDebugSession = isDebugSession;
    myServerConsole = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
    Disposer.register(this, myServerConsole);
    myModuleLogComponent = new ModuleLogComponent(project);
    Disposer.register(this, myModuleLogComponent);
    final SocketConnection<RemoteUiRequest,RemoteUiResponse> connection = uiConnection.getConnection();
    new RemoteUiLogManager(project, connection, this).registerHandlers();
    connection.registerHandler(FailureResponse.class, new AbstractResponseHandler<>() {
      @Override
      public void processResponse(@NotNull FailureResponse response) {
        delegate.print(response.getMessage(), ConsoleViewContentType.ERROR_OUTPUT);
      }
    });
    connection.registerHandler(InitializeResponse.class, new AbstractResponseHandler<>() {
      @Override
      public void processResponse(@NotNull InitializeResponse response) {
        final List<String> urls = response.getStartupUrlsList();
        String message = GwtBundle.message("console.message.dev.mode.initialized.startup.urls", urls.size());
        delegate.print(message + "\n", ConsoleViewContentType.SYSTEM_OUTPUT);
        final Condition<WebBrowser> suitableBrowsers =
          browser -> myUiConnection.getSdkVersion().isBrowserSupportedInDevMode(browser);
        for (String url : urls) {
          delegate.printHyperlink(url, new OpenUrlHyperlinkInfo(url, suitableBrowsers));
          delegate.print("\n", ConsoleViewContentType.SYSTEM_OUTPUT);
        }
      }
    });
  }

  public ConsoleView getServerConsole() {
    return myServerConsole;
  }

  public ConsoleView getMainConsole() {
    return getDelegate();
  }

  public ConsoleView getModuleConsole(@NotNull String moduleName) {
    return myModuleLogComponent.getOrCreateModuleConsole(moduleName);
  }

  @Override
  public void buildUi(RunnerLayoutUi layoutUi) {
    final Content mainContent = layoutUi.createContent(CONSOLE_CONTENT_ID, getComponent(),
                                                       GwtBundle.message("content.display.name.dev.mode"),
                                                       AllIcons.Debugger.Console,
                                                       getPreferredFocusableComponent());
    mainContent.setCloseable(false);
    RunContentBuilder.addAdditionalConsoleEditorActions(this, mainContent);
    int tabIndex = myDebugSession ? 1 : 0;
    layoutUi.addContent(mainContent, tabIndex++, PlaceInGrid.bottom, false);

    final Content modulesContent = layoutUi.createContent("GWT_REMOTE_UI_MODULES_LOG", myModuleLogComponent.getMainPanel(),
                                                          GwtBundle.message("content.display.name.modules"),
                                                          AllIcons.Debugger.Console,
                                                          null);
    modulesContent.setCloseable(false);
    layoutUi.addContent(modulesContent, tabIndex++, PlaceInGrid.center, false);

    final Content consoleContent = layoutUi.createContent("GWT_REMOTE_UI_SERVER_LOG", myServerConsole.getComponent(),
                                                          GwtBundle.message("content.display.name.server"),
                                                          AllIcons.Debugger.Console,
                                                          myServerConsole.getPreferredFocusableComponent());
    consoleContent.setCloseable(false);
    layoutUi.addContent(consoleContent, tabIndex, PlaceInGrid.right, false);
    layoutUi.getOptions().setMoveToGridActionEnabled(true);
  }
}
