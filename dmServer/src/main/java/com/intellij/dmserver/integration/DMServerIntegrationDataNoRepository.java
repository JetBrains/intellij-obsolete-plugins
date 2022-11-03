package com.intellij.dmserver.integration;

import com.intellij.javaee.appServers.appServerIntegrations.ApplicationServerPersistentData;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;

public class DMServerIntegrationDataNoRepository extends DMServerIntegrationStateBase implements ApplicationServerPersistentData {

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    XmlSerializer.deserializeInto(this, element);
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    XmlSerializer.serializeInto(this, element);
  }
}
