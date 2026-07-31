package com.intellij.gwt.run.remoteUi.responses;

public class FailureResponse extends RemoteUiResponse {
  private final String myMessage;

  public FailureResponse(int messageId, String message, String stackTrace) {
    super(messageId);
    myMessage = message;
  }

  public String getMessage() {
    return myMessage;
  }
}
