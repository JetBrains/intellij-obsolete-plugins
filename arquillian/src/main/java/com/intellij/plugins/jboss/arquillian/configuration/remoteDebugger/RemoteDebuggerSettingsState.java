package com.intellij.plugins.jboss.arquillian.configuration.remoteDebugger;

import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.NotNull;

public class RemoteDebuggerSettingsState {
  @NotNull
  @Attribute("transport")
  public RemoteDebuggerTransport transport = RemoteDebuggerTransport.Socket;

  @NotNull
  @Attribute("mode")
  public RemoteDebuggerMode mode = RemoteDebuggerMode.Attach;

  @NotNull
  @Attribute("host")
  public String host = "localhost";

  @NotNull
  @Attribute("port")
  public String port = "5005";

  @NotNull
  @Attribute("sharedMemoryAddress")
  public String sharedMemoryAddress = "javadebug";

  @Override
  public RemoteDebuggerSettingsState clone() {
    RemoteDebuggerSettingsState result = new RemoteDebuggerSettingsState();
    XmlSerializerUtil.copyBean(this, result);
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RemoteDebuggerSettingsState state = (RemoteDebuggerSettingsState)o;

    if (transport != state.transport) return false;
    if (mode != state.mode) return false;
    if (!host.equals(state.host)) return false;
    if (!port.equals(state.port)) return false;
    if (!sharedMemoryAddress.equals(state.sharedMemoryAddress)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = transport.hashCode();
    result = 31 * result + mode.hashCode();
    result = 31 * result + host.hashCode();
    result = 31 * result + port.hashCode();
    result = 31 * result + sharedMemoryAddress.hashCode();
    return result;
  }
}
