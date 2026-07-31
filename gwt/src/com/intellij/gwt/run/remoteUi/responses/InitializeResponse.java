package com.intellij.gwt.run.remoteUi.responses;

import java.util.List;

public class InitializeResponse extends RemoteUiResponse {
  private final List<String> myStartupUrlsList;

  public InitializeResponse(int messageId, List<String> startupUrlsList) {
    super(messageId);
    myStartupUrlsList = startupUrlsList;
  }

  public List<String> getStartupUrlsList() {
    return myStartupUrlsList;
  }
}
