package com.intellij.gwt.run.remoteUi;

import com.intellij.gwt.shell.remoteUi.generated.RemoteMessageProto;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.io.socketConnection.AbstractRequest;

import java.io.IOException;
import java.io.OutputStream;

import static com.intellij.gwt.shell.remoteUi.generated.RemoteMessageProto.Message.Request.ViewerRequest.RequestType.ADD_LOG;
import static com.intellij.gwt.shell.remoteUi.generated.RemoteMessageProto.Message.Request.ViewerRequest.RequestType.ADD_LOG_BRANCH;
import static com.intellij.gwt.shell.remoteUi.generated.RemoteMessageProto.Message.Request.ViewerRequest.RequestType.ADD_LOG_ENTRY;
import static com.intellij.gwt.shell.remoteUi.generated.RemoteMessageProto.Message.Request.ViewerRequest.RequestType.CAPABILITY_EXCHANGE;
import static com.intellij.gwt.shell.remoteUi.generated.RemoteMessageProto.Message.Request.ViewerRequest.RequestType.DISCONNECT_LOG;
import static com.intellij.gwt.shell.remoteUi.generated.RemoteMessageProto.Message.Request.ViewerRequest.RequestType.INITIALIZE;

public final class RemoteUiRequest implements AbstractRequest {
  private static final Logger LOG = Logger.getInstance(RemoteUiRequest.class);
  private final RemoteMessageProto.Message.Builder myBuilder;
  private final int myMessageId;

  private RemoteUiRequest(final RemoteMessageProto.Message.MessageType messageType, final int messageId) {
    myMessageId = messageId;
    myBuilder = RemoteMessageProto.Message.newBuilder()
      .setMessageType(messageType)
      .setMessageId(messageId);
  }

  public void setViewerResponseBuilder(RemoteMessageProto.Message.Response.ViewerResponse.Builder builder) {
    final RemoteMessageProto.Message.Response.Builder response = RemoteMessageProto.Message.Response.newBuilder();
    response.setViewerResponse(builder);
    myBuilder.setResponse(response);
  }

  public void sendMessage(OutputStream stream) throws IOException {
    final RemoteMessageProto.Message message = myBuilder.build();
    LOG.debug("Sending message: " + message);
    message.writeDelimitedTo(stream);
  }

  @Override
  public int getId() {
    return myMessageId;
  }

  public static RemoteUiRequest createRestartServerRequest() {
    final RemoteUiRequest request = new RemoteUiRequest(RemoteMessageProto.Message.MessageType.REQUEST, -1);
    request.myBuilder.setRequest(RemoteMessageProto.Message.Request.newBuilder()
                           .setServiceType(RemoteMessageProto.Message.Request.ServiceType.DEV_MODE)
                           .setDevModeRequest(RemoteMessageProto.Message.Request.DevModeRequest.newBuilder()
                             .setRequestType(RemoteMessageProto.Message.Request.DevModeRequest.RequestType.RESTART_WEB_SERVER)
                             .setRestartWebServer(RemoteMessageProto.Message.Request.DevModeRequest.RestartWebServer.newBuilder())));
    return request;
  }

  public static RemoteUiRequest createDummyResponseRequest(int messageId) {
    final RemoteUiRequest request = createResponseRequest(messageId);
    request.myBuilder.setResponse(RemoteMessageProto.Message.Response.newBuilder());
    return request;
  }

  private static RemoteUiRequest createResponseRequest(int messageId) {
    return new RemoteUiRequest(RemoteMessageProto.Message.MessageType.RESPONSE, messageId);
  }

  public static RemoteUiRequest createCapabilitiesResponse(final int messageId) {
    final RemoteUiRequest request = createResponseRequest(messageId);
    request.setViewerResponseBuilder(RemoteMessageProto.Message.Response.ViewerResponse.newBuilder()
                                       .setResponseType(RemoteMessageProto.Message.Response.ViewerResponse.ResponseType.CAPABILITY_EXCHANGE)
                                       .setCapabilityExchange(
                                         RemoteMessageProto.Message.Response.ViewerResponse.CapabilityExchange.newBuilder()
                                           .addCapabilities(createCapability(INITIALIZE))
                                           .addCapabilities(createCapability(CAPABILITY_EXCHANGE))
                                           .addCapabilities(createCapability(ADD_LOG))
                                           .addCapabilities(createCapability(ADD_LOG_BRANCH))
                                           .addCapabilities(createCapability(DISCONNECT_LOG))
                                           .addCapabilities(createCapability(ADD_LOG_ENTRY))));
    return request;
  }

  private static RemoteMessageProto.Message.Response.ViewerResponse.CapabilityExchange.Capability.Builder createCapability(final RemoteMessageProto.Message.Request.ViewerRequest.RequestType type) {
    return RemoteMessageProto.Message.Response.ViewerResponse.CapabilityExchange.Capability.newBuilder().setCapability(type);
  }

  public static RemoteUiRequest createAddLogBranchResponse(int handle, int messageId) {
    final RemoteUiRequest request = createResponseRequest(messageId);
    request.setViewerResponseBuilder(RemoteMessageProto.Message.Response.ViewerResponse.newBuilder()
                                       .setResponseType(RemoteMessageProto.Message.Response.ViewerResponse.ResponseType.ADD_LOG_BRANCH)
                                       .setAddLogBranch(
                                         RemoteMessageProto.Message.Response.ViewerResponse.AddLogBranch.newBuilder().setLogHandle(handle)));
    return request;
  }

  public static RemoteUiRequest createAddLogResponse(int handle, int messageId) {
    final RemoteUiRequest request = createResponseRequest(messageId);
    request.setViewerResponseBuilder(RemoteMessageProto.Message.Response.ViewerResponse.newBuilder()
                                       .setResponseType(RemoteMessageProto.Message.Response.ViewerResponse.ResponseType.ADD_LOG)
                                       .setAddLog(
                                         RemoteMessageProto.Message.Response.ViewerResponse.AddLog.newBuilder().setLogHandle(handle)));
    return request;
  }
}
