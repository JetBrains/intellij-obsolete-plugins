package com.intellij.gwt.run.remoteUi.responses;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.gwt.run.remoteUi.DevModeLogDescriptor;
import com.intellij.gwt.run.remoteUi.RemoteUiView;
import com.intellij.gwt.shell.remoteUi.generated.RemoteMessageProto;

public class AddModuleLogResponse extends AddLogResponseBase {
  private final RemoteMessageProto.Message.Request.ViewerRequest.AddLog.ModuleLog myModuleLog;

  public AddModuleLogResponse(int messageId, RemoteMessageProto.Message.Request.ViewerRequest.AddLog.ModuleLog moduleLog) {
    super(messageId);
    myModuleLog = moduleLog;
  }

  public RemoteMessageProto.Message.Request.ViewerRequest.AddLog.ModuleLog getModuleLog() {
    return myModuleLog;
  }

  @Override
  public DevModeLogDescriptor createLogDescriptor() {
    final String name = myModuleLog.getName();
    return new ModuleLogDescriptor(name);
  }

  private static class ModuleLogDescriptor extends DevModeLogDescriptor {
    private final String myName;

    ModuleLogDescriptor(String name) {
      myName = name;
    }

    @Override
    public ConsoleView getConsole(RemoteUiView view) {
      return view.getModuleConsole(myName);
    }
  }
}
