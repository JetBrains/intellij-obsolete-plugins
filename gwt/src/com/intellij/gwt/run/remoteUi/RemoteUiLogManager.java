package com.intellij.gwt.run.remoteUi;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.gwt.run.remoteUi.responses.AddLogBranchResponse;
import com.intellij.gwt.run.remoteUi.responses.AddLogEntryResponse;
import com.intellij.gwt.run.remoteUi.responses.AddLogResponseBase;
import com.intellij.gwt.run.remoteUi.responses.AddMainLogResponse;
import com.intellij.gwt.run.remoteUi.responses.AddModuleLogResponse;
import com.intellij.gwt.run.remoteUi.responses.AddWebServerLogResponse;
import com.intellij.gwt.run.remoteUi.responses.DisconnectLogResponse;
import com.intellij.gwt.run.remoteUi.responses.RemoteUiResponse;
import com.intellij.gwt.shell.remoteUi.generated.RemoteMessageProto;
import com.intellij.openapi.application.CoroutinesKt;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.io.socketConnection.AbstractResponseHandler;
import com.intellij.util.io.socketConnection.SocketConnection;
import com.intellij.util.ui.update.DebouncedUpdates;
import com.intellij.util.ui.update.UpdateQueue;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.Dispatchers;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class RemoteUiLogManager {
  private static final Logger LOG = Logger.getInstance(RemoteUiLogManager.class);
  private int myNextLogHandle;
  private final Object myLock = new Object();
  private final Int2ObjectMap<LogBranchDescriptor> myLogDescriptors = new Int2ObjectOpenHashMap<>();
  private final SocketConnection<RemoteUiRequest, RemoteUiResponse> myConnection;
  private final RemoteUiView myView;
  private final UpdateQueue<LogMessage> myLogUpdateQueue;

  public RemoteUiLogManager(Project project, SocketConnection<RemoteUiRequest, RemoteUiResponse> connection, RemoteUiView view) {
    myConnection = connection;
    myView = view;
    
    CoroutineScope scope = RemoteUiLogManagerScopeProvider.getInstance(project).childScope("GWT RemoteUI Log");
    Disposer.register(connection, () -> CoroutineScopeKt.cancel(scope, null));
    
    myLogUpdateQueue = DebouncedUpdates.<LogMessage>forScope(scope, "GWT RemoteUI Log updates", 300)
      .withContext(CoroutinesKt.getUI(Dispatchers.INSTANCE))
      .runBatched(this::processLogMessages);
  }

  public void registerHandlers() {
    myConnection.registerHandler(AddMainLogResponse.class, new AddLogResponseHandler<>());
    myConnection.registerHandler(AddWebServerLogResponse.class, new AddLogResponseHandler<>());
    myConnection.registerHandler(AddModuleLogResponse.class, new AddModuleLogResponseHandler());
    myConnection.registerHandler(AddLogBranchResponse.class, new AbstractResponseHandler<>() {
      @Override
      public void processResponse(@NotNull AddLogBranchResponse response) {
        final RemoteMessageProto.Message.Request.ViewerRequest.AddLogBranch addLogBranch = response.getAddLogBranch();
        addLogEntry(addLogBranch.getParentLogHandle(), addLogBranch.getLogData());
        int handle = createNewLogBranch(addLogBranch.getParentLogHandle());
        myConnection.sendRequest(RemoteUiRequest.createAddLogBranchResponse(handle, response.getMessageId()));
      }
    });
    myConnection.registerHandler(AddLogEntryResponse.class, new AbstractResponseHandler<>() {
      @Override
      public void processResponse(@NotNull AddLogEntryResponse response) {
        addLogEntry(response.getAddLogEntry().getLogHandle(), response.getAddLogEntry().getLogData());
        myConnection.sendRequest(RemoteUiRequest.createDummyResponseRequest(response.getMessageId()));
      }
    });
    myConnection.registerHandler(DisconnectLogResponse.class, new AbstractResponseHandler<>() {
      @Override
      public void processResponse(@NotNull DisconnectLogResponse response) {
        removeLog(response.getLogHandle());
        myConnection.sendRequest(RemoteUiRequest.createDummyResponseRequest(response.getMessageId()));
      }
    });
  }

  private void removeLog(int logHandle) {
    synchronized (myLock) {
      myLogDescriptors.remove(logHandle);
    }
  }

  private void addLogEntry(int logHandle, RemoteMessageProto.Message.Request.ViewerRequest.LogData data) {
    final String details = data.getDetails();
    addLogEntry(logHandle, data.getLevel() + ": " + data.getSummary() +
                           (StringUtil.isEmpty(details) ? "" : "\n" + details), "ERROR".equals(data.getLevel()));
  }

  private void addLogEntry(int logHandle, @NonNls String message, boolean error) {
    final LogBranchDescriptor descriptor;
    synchronized (myLock) {
      descriptor = myLogDescriptors.get(logHandle);
    }
    if (descriptor == null) {
      LOG.info("descriptor " + logHandle + " not found");
      return;
    }

    myLogUpdateQueue.queue(new LogMessage(descriptor, message, error));
  }

  private int createNewLogBranch(int parentLogHandle) {
    synchronized (myLock) {
      final LogBranchDescriptor parent = myLogDescriptors.get(parentLogHandle);
      if (parent == null) {
        LOG.info("parent not found: " + parentLogHandle);
        return createNewLog(DevModeLogDescriptor.MAIN_LOG_DESCRIPTOR, 0);
      }

      return createNewLog(parent.myRootDescriptor, parent.myIndent + 1);
    }
  }

  public int createNewLog(final DevModeLogDescriptor logDescriptor, int indent) {
    synchronized (myLock) {
      final int handle = myNextLogHandle++;
      myLogDescriptors.put(handle, new LogBranchDescriptor(logDescriptor, indent));
      return handle;
    }
  }

  private static final class LogBranchDescriptor {
    private final DevModeLogDescriptor myRootDescriptor;
    private final int myIndent;

    private LogBranchDescriptor(DevModeLogDescriptor rootDescriptor, int indent) {
      myRootDescriptor = rootDescriptor;
      myIndent = indent;
    }
  }

  private static final class LogMessage {
    private final LogBranchDescriptor myDescriptor;
    private final String myText;
    private final boolean myError;

    private LogMessage(LogBranchDescriptor descriptor, String text, boolean error) {
      myDescriptor = descriptor;
      myText = text;
      myError = error;
    }
  }

  private void processLogMessages(List<LogMessage> messages) {
    for (LogMessage message : messages) {
      final ConsoleView consoleView = message.myDescriptor.myRootDescriptor.getConsole(myView);
      final String indent = StringUtil.repeatSymbol(' ', 2 * message.myDescriptor.myIndent);
      final ConsoleViewContentType contentType = message.myError ? ConsoleViewContentType.ERROR_OUTPUT : ConsoleViewContentType.NORMAL_OUTPUT;
      consoleView.print(indent + message.myText + "\n", contentType);
    }
  }

  private class AddLogResponseHandler<T extends AddLogResponseBase> implements AbstractResponseHandler<T> {
    @Override
    public void processResponse(@NotNull T response) {
      doProcess(response);
    }

    protected int doProcess(T response) {
      final int handle = createNewLog(response.createLogDescriptor(), 0);
      myConnection.sendRequest(RemoteUiRequest.createAddLogResponse(handle, response.getMessageId()));
      return handle;
    }
  }

  private class AddModuleLogResponseHandler extends AddLogResponseHandler<AddModuleLogResponse> {
    @Override
    public void processResponse(@NotNull AddModuleLogResponse response) {
      final int handle = doProcess(response);
      final RemoteMessageProto.Message.Request.ViewerRequest.AddLog.ModuleLog moduleLog = response.getModuleLog();
      addLogEntry(handle, "Loading module: " + moduleLog.getName(), false);
      final int branch = createNewLogBranch(handle);
      final String url = moduleLog.getUrl();
      if (url != null) {
        addLogEntry(branch, "Top URL: " + url, false);
      }
      addLogEntry(branch, "User agent: " + moduleLog.getUserAgent(), false);
      addLogEntry(branch, "Remote host: " + moduleLog.getRemoteHost(), false);
      final String tabKey = moduleLog.getTabKey();
      if (tabKey != null) {
        addLogEntry(branch, "Tab key: " + tabKey, false);
      }
      final String sessionKey = moduleLog.getSessionKey();
      if (sessionKey != null) {
        addLogEntry(branch, "Session key: " + sessionKey, false);
      }
    }
  }
}
