package com.intellij.gwt.run.remoteUi.responses;

import com.intellij.gwt.run.remoteUi.UnknownRemoteUiResponse;
import com.intellij.gwt.shell.remoteUi.generated.RemoteMessageProto;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.io.socketConnection.ResponseReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;

public class RemoteUiResponseReader implements ResponseReader<RemoteUiResponse> {
  private static final Logger LOG = Logger.getInstance(RemoteUiResponseReader.class);
  private final InputStream myInput;

  public RemoteUiResponseReader(InputStream input) {
    myInput = input;
  }

  @Override
  public RemoteUiResponse readResponse() throws IOException, InterruptedException {
    final RemoteMessageProto.Message message = RemoteMessageProto.Message.parseDelimitedFrom(myInput);
    if (message == null) {
      LOG.debug("Null message received, stopping");
      return null;
    }

    final RemoteUiResponse response = fromMessage(message);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Received: " + response);
    }
    return response != null ? response : new UnknownRemoteUiResponse(message.getMessageId());
  }

  private static RemoteUiResponse fromMessage(@NotNull RemoteMessageProto.Message message) throws InterruptedException {
    return switch (message.getMessageType()) {
      case REQUEST -> fromRequest(message.getRequest(), message.getMessageId());
      case RESPONSE -> null;
      case FAILURE -> {
        final RemoteMessageProto.Message.Failure failure = message.getFailure();
        yield new FailureResponse(message.getMessageId(), failure.getMessage(), failure.getStackTrace());
      }
    };
  }

  private static @Nullable RemoteUiResponse fromRequest(RemoteMessageProto.Message.Request request, int messageId) {
    if (request.getServiceType() == RemoteMessageProto.Message.Request.ServiceType.DEV_MODE) {
      LOG.info("Dev mode requests aren't processed");
      return null;
    }

    final RemoteMessageProto.Message.Request.ViewerRequest viewerRequest = request.getViewerRequest();
    return switch (viewerRequest.getRequestType()) {
      case CAPABILITY_EXCHANGE -> new CapabilityExchangeResponse(messageId);
      case ADD_LOG -> fromAddLog(viewerRequest.getAddLog(), messageId);
      case ADD_LOG_BRANCH -> new AddLogBranchResponse(messageId, viewerRequest.getAddLogBranch());
      case ADD_LOG_ENTRY -> new AddLogEntryResponse(messageId, viewerRequest.getAddLogEntry());
      case DISCONNECT_LOG -> new DisconnectLogResponse(messageId, viewerRequest.getDisconnectLog().getLogHandle());
      case INITIALIZE -> new InitializeResponse(messageId, viewerRequest.getInitialize().getStartupURLsList());
    };
  }

  private static RemoteUiResponse fromAddLog(RemoteMessageProto.Message.Request.ViewerRequest.AddLog addLog, int messageId) {
    return switch (addLog.getType()) {
      case MAIN -> new AddMainLogResponse(messageId);
      case MODULE -> new AddModuleLogResponse(messageId, addLog.getModuleLog());
      case WEB_SERVER -> new AddWebServerLogResponse(messageId);
    };
  }
}
