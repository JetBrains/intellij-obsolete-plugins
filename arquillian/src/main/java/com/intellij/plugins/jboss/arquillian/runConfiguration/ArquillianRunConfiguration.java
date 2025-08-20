package com.intellij.plugins.jboss.arquillian.runConfiguration;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.*;
import org.jdom.Element;

@SuppressWarnings("deprecation")
public class ArquillianRunConfiguration implements JDOMExternalizable {
  private static final Logger LOG = Logger.getInstance(ArquillianRunConfiguration.class);

  public @NlsSafe String containerStateName;

  public ArquillianRunConfiguration() {
  }

  public ArquillianRunConfiguration(@NlsSafe String containerStateName) {
    this.containerStateName = containerStateName;
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    DefaultJDOMExternalizer.readExternal(this, element);
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    DefaultJDOMExternalizer.writeExternal(this, element);
  }

  public @NlsSafe String getContainerStateName() {
    return containerStateName;
  }

  public void setContainerStateName(@NlsSafe String containerStateName) {
    this.containerStateName = containerStateName;
  }
}
