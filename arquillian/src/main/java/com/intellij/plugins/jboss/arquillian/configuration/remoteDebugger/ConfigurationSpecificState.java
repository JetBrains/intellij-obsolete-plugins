package com.intellij.plugins.jboss.arquillian.configuration.remoteDebugger;

import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianState;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.NotNull;

@Tag("configurationSpecific")
public class ConfigurationSpecificState implements ArquillianState {
  @Attribute("enabled")
  public boolean remoteDebuggingEnabled = false;

  @NotNull
  @Attribute("runContainerQualifier")
  public String runContainerQualifier = "";

  @NotNull
  @Attribute("debugContainerQualifier")
  public String debugContainerQualifier = "";

  @NotNull
  @Property(surroundWithTag = false)
  public RemoteDebuggerSettingsState settings = new RemoteDebuggerSettingsState();

  @Override
  public ConfigurationSpecificState clone() {
    ConfigurationSpecificState result = new ConfigurationSpecificState();
    XmlSerializerUtil.copyBean(this, result);
    result.settings = settings.clone();
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ConfigurationSpecificState state = (ConfigurationSpecificState)o;

    if (remoteDebuggingEnabled != state.remoteDebuggingEnabled) return false;
    if (!runContainerQualifier.equals(state.runContainerQualifier)) return false;
    if (!settings.equals(state.settings)) return false;
    if (!debugContainerQualifier.equals(state.debugContainerQualifier)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = (remoteDebuggingEnabled ? 1 : 0);
    result = 31 * result + runContainerQualifier.hashCode();
    result = 31 * result + settings.hashCode();
    result = 31 * result + debugContainerQualifier.hashCode();
    return result;
  }
}
