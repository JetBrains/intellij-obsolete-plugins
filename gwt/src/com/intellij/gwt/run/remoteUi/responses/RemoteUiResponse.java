package com.intellij.gwt.run.remoteUi.responses;

import com.intellij.util.io.socketConnection.AbstractResponse;

public abstract class RemoteUiResponse implements AbstractResponse {
  private final int myMessageId;

  public RemoteUiResponse(int messageId) {
    myMessageId = messageId;
  }

  public int getMessageId() {
    return myMessageId;
  }
}
