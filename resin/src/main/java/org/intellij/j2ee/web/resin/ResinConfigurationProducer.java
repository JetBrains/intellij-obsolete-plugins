package org.intellij.j2ee.web.resin;

import com.intellij.javaee.appServers.run.configuration.J2EEConfigurationProducer;

public class ResinConfigurationProducer extends J2EEConfigurationProducer {
  public ResinConfigurationProducer() {
    super(ResinConfigurationType.getInstance());
  }
}
