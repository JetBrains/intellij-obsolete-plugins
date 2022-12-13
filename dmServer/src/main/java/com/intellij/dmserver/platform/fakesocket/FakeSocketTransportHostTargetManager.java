package com.intellij.dmserver.platform.fakesocket;


import com.intellij.javaee.transport.TransportHostTargetManager;

public final class FakeSocketTransportHostTargetManager extends TransportHostTargetManager<FakeSocketTransportHostTarget> {

  private static final FakeSocketTransportHostTargetManager ourInstance = new FakeSocketTransportHostTargetManager();

  public static FakeSocketTransportHostTargetManager getInstance() {
    return ourInstance;
  }

  private FakeSocketTransportHostTargetManager() {

  }

  @Override
  protected FakeSocketTransportHostTarget doCreateHostTarget() {
    return new FakeSocketTransportHostTarget();
  }
}
