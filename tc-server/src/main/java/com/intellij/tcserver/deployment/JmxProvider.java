package com.intellij.tcserver.deployment;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.tcserver.deployment.exceptions.FailedToConnectJmxException;
import com.intellij.tcserver.deployment.exceptions.FailedToInvokeJmxException;
import com.intellij.tcserver.deployment.exceptions.NotAllowedToConnectException;
import com.intellij.tcserver.util.TcServerBundle;
import org.jetbrains.annotations.NonNls;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.TreeMap;

public class JmxProvider {
  @NonNls private static final String DEPLOY_APPLICATION_METHOD = "deployApplication";
  @NonNls private static final String UNDEPLOY_APPLICATION_METHOD = "undeployApplication";
  @NonNls private static final String GET_APPLICATION_STATE_METHOD = "getApplicationState";

  private static final Logger MY_LOG = Logger.getInstance(JmxProvider.class);
  private static final ObjectName MY_DEPLOYER_SERVICE_OBJECT_NAME;

  static {
    ObjectName objectName = null;
    @NonNls final String name = "tcServer:type=Serviceability,name=Deployer";
    try {
      objectName = new ObjectName(name);
    }
    catch (MalformedObjectNameException e) {
      MY_LOG.error("JMX service name \"" + name + "\" is malformed", e);
    }
    MY_DEPLOYER_SERVICE_OBJECT_NAME = objectName;
  }

  private JMXConnector myJmxConnector;
  private final int myPort;
  private final String myHost;
  private final Map<String, Object> myEnvMap;

  public JmxProvider(String host, int port) {
    myPort = port;
    myHost = host;
    myEnvMap = null;
  }

  public JmxProvider(String host, int port, String role, String password) {
    myPort = port;
    myHost = host;
    myEnvMap = new TreeMap<>();
    String[] credentials = {role, password};
    myEnvMap.put(JMXConnector.CREDENTIALS, credentials);
  }

  private MBeanServerConnection getConnection() throws FailedToConnectJmxException, NotAllowedToConnectException {
    JMXServiceURL url;
    @NonNls final String urlString = "service:jmx:rmi:///jndi/rmi://" + myHost + ":" + myPort + "/jmxrmi";
    try {
      url = new JMXServiceURL(urlString);
    }
    catch (MalformedURLException e) {
      MY_LOG.error("Malformed jmx URL: " + urlString, e);
      throw new FailedToConnectJmxException(TcServerBundle.message("jmxProvider.malformedUrl", urlString));
    }

    JMXConnector jmxc;

    try {
      jmxc = JMXConnectorFactory.connect(url, myEnvMap);
    }
    catch (IOException e) {
      MY_LOG.info("Failed to connect JMXConnectorFactory", e);
      throw new FailedToConnectJmxException(TcServerBundle.message("jmxProvider.failedToConnect", urlString));
    }
    catch (SecurityException e) {
      MY_LOG.info("Not allowed to connect JMXConnectorFactory", e);
      throw new NotAllowedToConnectException();
    }

    MY_LOG.assertTrue(myJmxConnector == null);
    myJmxConnector = jmxc;

    try {
      return jmxc.getMBeanServerConnection();
    }
    catch (IOException e) {
      MY_LOG.info("Failed to get MBeanServerConnection", e);
      closeJmxConnection();
      throw new FailedToConnectJmxException(TcServerBundle.message("jmxProvider.failedToConnect", urlString));
    }
  }

  private void closeJmxConnection() {
    if (myJmxConnector != null) {
      try {
        myJmxConnector.close();
      }
      catch (IOException e) {
        MY_LOG.info("Failed to close JMXConnector", e);
      }
      myJmxConnector = null;
    }
  }

  public void ping() throws NotAllowedToConnectException, FailedToConnectJmxException {
    getConnection();
    closeJmxConnection();
  }

  public void deployApplication(TcServerDeploymentModel deploymentModel, String sourcePath)
    throws FailedToConnectJmxException, FailedToInvokeJmxException, NotAllowedToConnectException {
    String serverService = deploymentModel.getServerService();
    String serverHost = deploymentModel.getServerHost();
    String path = deploymentModel.getWebPath();
    MY_LOG.info("deploy service: " + serverService + ", serverHost " + serverHost + ", path " + path + ", sourcePath " + sourcePath);

    callJmx(mbsc -> mbsc.invoke(MY_DEPLOYER_SERVICE_OBJECT_NAME,
                                DEPLOY_APPLICATION_METHOD,
                                new Object[]{serverService, serverHost, path, sourcePath},
                                new String[]{"java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String"}));
  }

  public void undeployApplication(TcServerDeploymentModel deploymentModel)
    throws FailedToConnectJmxException, FailedToInvokeJmxException, NotAllowedToConnectException {
    String serverService = deploymentModel.getServerService();
    String serverHost = deploymentModel.getServerHost();
    String path = deploymentModel.getWebPath();
    MY_LOG.info("undeploy service: " + serverService + ", serverHost " + serverHost + ", path " + path);

    callJmx(mbsc -> mbsc.invoke(MY_DEPLOYER_SERVICE_OBJECT_NAME,
                                UNDEPLOY_APPLICATION_METHOD,
                                new Object[]{serverService, serverHost, path},
                                new String[]{"java.lang.String", "java.lang.String", "java.lang.String"}));
  }

  @FunctionalInterface
  private interface JmxVoidOperation {
    void performOn(MBeanServerConnection mbsc) throws InstanceNotFoundException, IOException, ReflectionException, MBeanException;
  }

  private void callJmx(JmxVoidOperation op)
    throws FailedToConnectJmxException, FailedToInvokeJmxException, NotAllowedToConnectException {
    MBeanServerConnection mbsc = getConnection();
    try {
      op.performOn(mbsc);
    }
    catch (InstanceNotFoundException | IOException | ReflectionException | MBeanException e) {
      MY_LOG.info(e);
      throw new FailedToInvokeJmxException();
    }
    catch (RuntimeException e) {
      MY_LOG.info(e);
      throw new FailedToInvokeJmxException(e.getMessage());
    }
    finally {
      closeJmxConnection();
    }
  }

  public String getApplicationState(TcServerDeploymentModel deploymentModel)
    throws FailedToConnectJmxException, FailedToInvokeJmxException, NotAllowedToConnectException {
    String serverService = deploymentModel.getServerService();
    String serverHost = deploymentModel.getServerHost();
    String path = deploymentModel.getWebPath();
    MY_LOG.info("getApplicationState service: " + serverService + ", serverHost " + serverHost + ", path " + path);
    MBeanServerConnection mbsc = getConnection();
    Object[] parameters = new Object[]{serverService, serverHost, path};
    String[] signature = new String[]{"java.lang.String", "java.lang.String", "java.lang.String"};
    try {
      return (String)mbsc.invoke(MY_DEPLOYER_SERVICE_OBJECT_NAME, GET_APPLICATION_STATE_METHOD, parameters, signature);
    }
    catch (JMException | IOException e) {
      MY_LOG.info(e);
      throw new FailedToInvokeJmxException();
    }
    catch (RuntimeException e) {
      MY_LOG.info(e);
      throw new FailedToInvokeJmxException(e.getMessage());
    }
    finally {
      closeJmxConnection();
    }
  }

  public void shutdownServer() {
    try {
      ObjectName name = ObjectName.getInstance("Catalina:type=Service");
      MBeanServerConnection connection = getConnection();
      if (connection != null) {
        connection.invoke(name, "stop", null, null);
      }
    }
    catch (JMException | FailedToConnectJmxException | NotAllowedToConnectException | IOException e) {
      MY_LOG.info(e);
    }
    finally {
      closeJmxConnection();
    }
  }
}
