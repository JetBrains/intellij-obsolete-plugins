package com.intellij.gwt.run.remoteUi;

import com.intellij.execution.ui.ConsoleView;

public abstract class DevModeLogDescriptor {
  public static final DevModeLogDescriptor MAIN_LOG_DESCRIPTOR = new DevModeLogDescriptor() {
    @Override
    public ConsoleView getConsole(RemoteUiView view) {
      return view.getMainConsole();
    }
  };
  public static final DevModeLogDescriptor SERVER_LOG_DESCRIPTOR = new DevModeLogDescriptor() {
    @Override
    public ConsoleView getConsole(RemoteUiView view) {
      return view.getServerConsole();
    }
  };

  public abstract ConsoleView getConsole(RemoteUiView view);
}
