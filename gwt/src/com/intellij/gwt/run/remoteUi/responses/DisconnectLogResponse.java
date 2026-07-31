package com.intellij.gwt.run.remoteUi.responses;

public class DisconnectLogResponse extends RemoteUiResponse {
  private final int myLogHandle;

  public DisconnectLogResponse(int messageId, int logHandle) {
    super(messageId);
    myLogHandle = logHandle;
  }

  public int getLogHandle() {
    return myLogHandle;
  }
}
