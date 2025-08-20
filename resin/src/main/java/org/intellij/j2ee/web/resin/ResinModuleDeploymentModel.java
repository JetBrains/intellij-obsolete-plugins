/*
 * Copyright 2000-2005 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.intellij.j2ee.web.resin;

import com.intellij.javaee.appServers.context.DeploymentModelContext;
import com.intellij.javaee.appServers.deployment.DeploymentModel;
import com.intellij.javaee.appServers.deployment.DeploymentSource;
import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;

public class ResinModuleDeploymentModel extends DeploymentModel implements DeploymentModelContext {

  private ResinModuleDeploymentModelData myData = new ResinModuleDeploymentModelData();

  public ResinModuleDeploymentModel(CommonModel commonModel, DeploymentSource source) {
    super(commonModel, source);
  }

  @Override
  public boolean isDefaultContextRoot() {
    return isDefaultContextPath();
  }

  @Override
  public String getContextRoot() {
    return getContextPath();
  }

  public boolean isDefaultContextPath() {
    return myData.isDefaultContextPath();
  }

  public void setDefaultContextPath(boolean defaultContextPath) {
    myData.setDefaultContextPath(defaultContextPath);
  }

  public String getContextPath() {
    return myData.getContextPath();
  }

  public void setContextPath(String contextPath) {
    myData.setContextPath(contextPath);
  }

  public String getHost() {
    return myData.getHost();
  }

  public void setHost(String host) {
    myData.setHost(host);
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    super.writeExternal(element);
    XmlSerializer.serializeInto(myData, element, new SkipDefaultValuesSerializationFilters());
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    super.readExternal(element);
    myData = new ResinModuleDeploymentModelData();
    XmlSerializer.deserializeInto(myData, element);
  }

  public static class ResinModuleDeploymentModelData {

    private String myContextPath = "/";
    private String myHost = "";
    private boolean myDefaultContextPath = true;

    public boolean isDefaultContextPath() {
      return myDefaultContextPath;
    }

    public void setDefaultContextPath(boolean defaultContextPath) {
      myDefaultContextPath = defaultContextPath;
    }

    public String getContextPath() {
      return myContextPath;
    }

    public void setContextPath(String contextPath) {
      myContextPath = contextPath;
    }

    public String getHost() {
      return myHost;
    }

    public void setHost(String host) {
      myHost = host;
    }
  }
}
