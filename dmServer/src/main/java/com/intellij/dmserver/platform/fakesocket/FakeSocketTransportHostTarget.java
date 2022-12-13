package com.intellij.dmserver.platform.fakesocket;

import com.intellij.javaee.transport.TransportHostTargetManaged;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.List;

public class FakeSocketTransportHostTarget extends TransportHostTargetManaged {

  private String myPortIn;

  private String myPortOut;

  public String getPortIn() {
    return myPortIn;
  }

  public void setPortIn(String portIn) {
    myPortIn = portIn;
  }

  public String getPortOut() {
    return myPortOut;
  }

  public void setPortOut(String portOut) {
    myPortOut = portOut;
  }

  @Override
  public boolean transfer(Project project, List<VirtualFile> files) {
    System.out.println("FakeSocketTransportHostTarget is asked to transfer files");
    return true;
  }

  @Override
  public boolean delete(Project project, List<VirtualFile> files) {
    System.out.println("FakeSocketTransportHostTarget is asked to delete files");
    return true;
  }
}
