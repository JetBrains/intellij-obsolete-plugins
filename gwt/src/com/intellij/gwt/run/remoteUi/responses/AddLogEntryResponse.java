package com.intellij.gwt.run.remoteUi.responses;

import com.intellij.gwt.shell.remoteUi.generated.RemoteMessageProto;

public class AddLogEntryResponse extends RemoteUiResponse {
  private final RemoteMessageProto.Message.Request.ViewerRequest.AddLogEntry myAddLogEntry;

  public AddLogEntryResponse(int messageId, RemoteMessageProto.Message.Request.ViewerRequest.AddLogEntry addLogEntry) {
    super(messageId);
    myAddLogEntry = addLogEntry;
  }

  public RemoteMessageProto.Message.Request.ViewerRequest.AddLogEntry getAddLogEntry() {
    return myAddLogEntry;
  }
}
