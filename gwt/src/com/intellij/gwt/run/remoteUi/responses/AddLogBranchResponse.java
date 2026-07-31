package com.intellij.gwt.run.remoteUi.responses;

import com.intellij.gwt.shell.remoteUi.generated.RemoteMessageProto;

public class AddLogBranchResponse extends RemoteUiResponse {
  private final RemoteMessageProto.Message.Request.ViewerRequest.AddLogBranch myAddLogBranch;

  public AddLogBranchResponse(int messageId, RemoteMessageProto.Message.Request.ViewerRequest.AddLogBranch addLogBranch) {
    super(messageId);
    myAddLogBranch = addLogBranch;
  }

  public RemoteMessageProto.Message.Request.ViewerRequest.AddLogBranch getAddLogBranch() {
    return myAddLogBranch;
  }
}
