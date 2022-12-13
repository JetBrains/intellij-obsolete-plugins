package com.intellij.dmserver.platform.fakesocket;

import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.javaee.transport.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FakeSocketTransportService implements TransportService {

  public static final TransportType SOCKET_TYPE = new TransportTypeImpl(DmServerBundle.message("FakeSocketTransportService.socket")) {

    @Override
    public TransportHostTargetEditor createTargetEditor() {
      return new FakeSocketTransportHostTargetEditor();
    }
  };

  private final TransportHost[] myHosts =
    new TransportHost[]{new FakeSocketTransportHost("Socket-Host#1"), new FakeSocketTransportHost("Socket-Host#2")};

  @Override
  public List<TransportType> getTypes() {
    return Collections.singletonList(SOCKET_TYPE);
  }

  @Override
  public List<TransportHost> getHosts(@Nullable Project project) {
    return Arrays.asList(myHosts);
  }

  @Override
  public TransportHost editHostsOfType(TransportType type, TransportHost selectedHost, Project project) {
    Messages.showInfoMessage(DmServerBundle.message("FakeSocketTransportService.message.will.select.socket.host"),
                             DmServerBundle.message("FakeSocketTransportService.title.edit.hosts"));
    return myHosts[0];
  }
}
