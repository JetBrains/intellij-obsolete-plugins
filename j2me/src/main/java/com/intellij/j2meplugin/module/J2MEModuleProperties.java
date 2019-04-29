/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.module;

import com.intellij.j2meplugin.module.settings.MobileApplicationType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleServiceManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.WriteExternalException;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

public class J2MEModuleProperties implements JDOMExternalizable {
  private final Module myModule;
  private String myResourcePath;
  private MobileApplicationType myMobileApplicationType;
  //private DeploymentConnection myDeploymentConnection;

  @NonNls
  private final static String RESOURCE = "resourceRoot";
  @NonNls
  private final static String PATH = "path";

  @NonNls
  private final static String APPLICATION_TYPE = "mobileApplicationType";
  @NonNls
  private final static String TYPE_NAME = "name";

  /*private final static String DEPLOY = "deployConnection";
  private final static String SERVER = "server";
  private final static String REMOTE_PATH = "remotePath";
  private final static String USERNAME = "username";
  private final static String PASSWORD = "password";
  private final static String TRANSFER_MODE = "transferMode";*/


  public J2MEModuleProperties(Module module) {
    myModule = module;
  }

  public static J2MEModuleProperties getInstance(Module module) {
    return ModuleType.is(module, J2MEModuleType.getInstance()) ? ModuleServiceManager.getService(module, J2MEModuleProperties.class) : null;
  }

  public String getResourcePath() {
    return myResourcePath;
  }

  public void setResourcePath(String resourcePath) {
    myResourcePath = resourcePath;
  }

  @Nullable
  public MobileApplicationType getMobileApplicationType() {
    return myMobileApplicationType;
  }

  public void setMobileApplicationType(MobileApplicationType mobileApplicationType) {
    myMobileApplicationType = mobileApplicationType;
  }


  /* public DeploymentConnection getDeploymentConnection() {
     return myDeploymentConnection;
   }

   public void setDeploymentConnection(DeploymentConnection deploymentConnection) {
     myDeploymentConnection = deploymentConnection;
   }

   public DeploymentConnection createDeploymentConnection(String server,
                                                          String remotePath,
                                                          String username,
                                                          String password,
                                                          int transferMode) {
     return new DeploymentConnection(server, remotePath, username, password, transferMode);
   }*/

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    Element resource = element.getChild(RESOURCE);
    if (resource != null) {
      myResourcePath = resource.getAttributeValue(PATH);
    }
    Element type = element.getChild(APPLICATION_TYPE);
    if (type != null) {
      final String name = type.getAttributeValue(TYPE_NAME);
      myMobileApplicationType = MobileModuleUtil.getMobileApplicationTypeByName(name);
    }

    /* Element connection = element.getChild(DEPLOY);
     if (connection != null) {
       myDeploymentConnection = createDeploymentConnection(connection.getAttributeValue(SERVER),
                                                           connection.getAttributeValue(REMOTE_PATH),
                                                           connection.getAttributeValue(USERNAME),
                                                           connection.getAttributeValue(PASSWORD),
                                                           Integer.parseInt(connection.getAttributeValue(TRANSFER_MODE)));
     }*/

  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    if (myResourcePath != null) {
      Element resource = new Element(RESOURCE);
      resource.setAttribute(PATH, myResourcePath);
      element.addContent(resource);
    }
    if (myMobileApplicationType != null) {
      Element type = new Element(APPLICATION_TYPE);
      type.setAttribute(TYPE_NAME, myMobileApplicationType.getName());
      element.addContent(type);
    }

    /*if (myDeploymentConnection != null) {
      Element connection = new Element(DEPLOY);
      connection.setAttribute(SERVER, myDeploymentConnection.getServer());
      connection.setAttribute(REMOTE_PATH, myDeploymentConnection.getRemotePath());
      connection.setAttribute(USERNAME, myDeploymentConnection.getUsername());
      connection.setAttribute(PASSWORD, myDeploymentConnection.getPassword()); //todo
      connection.setAttribute(TRANSFER_MODE, new Integer(myDeploymentConnection.getTransferMode()).toString());
      element.addContent(connection);
    }*/
  }

  /*public static class DeploymentConnection {
    public static final int AUTO = 1;
    public static final int ACSII = 2;
    public static final int BINARY = 3;

    private String myServer;
    private String myRemotePath;
    private String myUsername;
    private String myPassword;

    private int myTransferMode;

    private DeploymentConnection(String server,
                                 String remotePath,
                                 String username,
                                 String password,
                                 int transferMode) {
      myServer = server;
      myRemotePath = remotePath;
      myUsername = username;
      myPassword = password;
      myTransferMode = transferMode;
    }

    public String getServer() {
      return myServer;
    }

    public void setServer(String server) {
      myServer = server;
    }

    public String getRemotePath() {
      return myRemotePath;
    }

    public void setRemotePath(String remotePath) {
      myRemotePath = remotePath;
    }

    public String getUsername() {
      return myUsername;
    }

    public void setUsername(String username) {
      myUsername = username;
    }

    public String getPassword() {
      return myPassword;
    }

    public void setPassword(String password) {
      myPassword = password;
    }

    public int getTransferMode() {
      return myTransferMode;
    }

    public void setTransferMode(int transferMode) {
      myTransferMode = transferMode;
    }
  }*/
}
