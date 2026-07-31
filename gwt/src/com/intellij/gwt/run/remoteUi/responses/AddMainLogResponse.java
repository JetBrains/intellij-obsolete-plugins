package com.intellij.gwt.run.remoteUi.responses;

import com.intellij.gwt.run.remoteUi.DevModeLogDescriptor;

public class AddMainLogResponse extends AddLogResponseBase {
  public AddMainLogResponse(int messageId) {
    super(messageId);
  }

  @Override
  public DevModeLogDescriptor createLogDescriptor() {
    return DevModeLogDescriptor.MAIN_LOG_DESCRIPTOR;
  }
}
