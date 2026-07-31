package com.intellij.gwt.make;

import com.intellij.compiler.server.CustomBuilderMessageHandler;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.make.report.CompileReportsHistory;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.MessageView;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.build.GwtBuilderMessages;

public class GwtBuilderMessageHandler implements CustomBuilderMessageHandler {
  private static final Key<String> GWT_MODULE_NAME = Key.create("GWT_MODULE_NAME");
  private final Project myProject;
  private boolean myListenerAdded;

  public GwtBuilderMessageHandler(Project project) {
    myProject = project;
  }

  public static GwtBuilderMessageHandler getInstance(@NotNull Project project) {
    return project.getService(GwtBuilderMessageHandler.class);
  }

  public void installExternalGwtBuilderListener() {
    if (myListenerAdded) return;
    myListenerAdded = true;

    myProject.getMessageBus().connect().subscribe(CustomBuilderMessageHandler.TOPIC, this);
  }

  public void showCompilerOutput(final ConsoleView consoleView, final String gwtModuleName) {
    ApplicationManager.getApplication().invokeLater(() -> {
      final MessageView messageView = MessageView.getInstance(myProject);
      final String name = GwtBundle.message("tab.title.0.gwt.output", StringUtil.getShortName(gwtModuleName));
      final Content content = ContentFactory.getInstance().createContent(consoleView.getComponent(), name, true);
      content.putUserData(GWT_MODULE_NAME, gwtModuleName);
      final ContentManager contentManager = messageView.getContentManager();
      contentManager.addContent(content);
      contentManager.setSelectedContent(content);
      for (Content content2 : contentManager.getContents()) {
        if (content2.isPinned() || content2.equals(content)) continue;

        final String gwtModuleName2 = content2.getUserData(GWT_MODULE_NAME);
        if (gwtModuleName.equals(gwtModuleName2)) {
          contentManager.removeContent(content2, true);
        }
      }
      ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.MESSAGES_WINDOW);
      if (toolWindow != null && !toolWindow.isActive()) {
        toolWindow.activate(null, false);
      }
    });
  }

  @Override
  public void messageReceived(String builderId, String messageType, String messageText) {
    if (!GwtBuilderMessages.BUILDER_ID.equals(builderId)) {
      return;
    }

    if (messageType.equals(GwtBuilderMessages.LOGGING_STARTED_TYPE)) {
      final TextConsoleBuilder builder = TextConsoleBuilderFactory.getInstance().createBuilder(myProject);
      builder.setViewer(true);
      final ConsoleView consoleView = builder.getConsole();
      final String moduleName = messageText;
      showCompilerOutput(consoleView, moduleName);
      final MessageBusConnection connection = myProject.getMessageBus().connect();
      connection.subscribe(TOPIC, new CustomBuilderMessageHandler() {
        @Override
        public void messageReceived(String builderId, String messageType, String messageText) {
          if (builderId.equals(GwtBuilderMessages.BUILDER_ID)) {
            if (messageType.equals(GwtBuilderMessages.LOGGING_FINISHED_TYPE)) {
              connection.disconnect();
            }
            else if (messageType.equals(moduleName)) {
              consoleView.print(messageText, ConsoleViewContentType.NORMAL_OUTPUT);
            }
          }
        }
      });
    }
    else if (messageType.startsWith(GwtBuilderMessages.COMPILE_REPORT_PREFIX)) {
      String moduleName = StringUtil.trimStart(messageType, GwtBuilderMessages.COMPILE_REPORT_PREFIX);
      CompileReportsHistory.getInstance(myProject).updateReport(moduleName, System.currentTimeMillis(), messageText);
    }
  }
}
