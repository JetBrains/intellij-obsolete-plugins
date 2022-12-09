package com.intellij.dmserver.platform.fakesocket;

import com.intellij.javaee.transport.TransportHost;
import com.intellij.javaee.transport.TransportHostTarget;
import com.intellij.javaee.transport.TransportTarget;
import com.intellij.javaee.transport.TransportType;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;

public class FakeSocketTransportHost implements TransportHost {

  @NonNls
  private static final String ID_PREFIX = "Socket.HostID#";

  @Nls private final String myName;

  public FakeSocketTransportHost(@Nls String name) {
    myName = name;
  }

  @Override
  public TransportType getType() {
    return FakeSocketTransportService.SOCKET_TYPE;
  }

  @Override
  public String getId() {
    return ID_PREFIX + myName;
  }

  @Override
  @Nls
  public String getName() {
    return myName;
  }

  @Override
  public TransportHostTarget findOrCreateHostTarget(TransportTarget target) {
    return FakeSocketTransportHostTargetManager.getInstance().findOrCreateHostTarget(getId(), target);
  }
}
