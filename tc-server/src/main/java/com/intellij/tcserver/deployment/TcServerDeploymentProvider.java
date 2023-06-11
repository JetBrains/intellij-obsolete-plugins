package com.intellij.tcserver.deployment;

import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.javaee.appServers.deployment.*;
import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.javaee.appServers.serverInstances.J2EEServerInstance;
import com.intellij.javaee.util.DeployStateChecker;
import com.intellij.javaee.util.ServerInstancePoller;
import com.intellij.javaee.web.artifact.WebArtifactUtil;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.tcserver.deployment.exceptions.FailedToConnectJmxException;
import com.intellij.tcserver.deployment.exceptions.FailedToInvokeJmxException;
import com.intellij.tcserver.deployment.exceptions.NotAllowedToConnectException;
import com.intellij.tcserver.server.instance.TcServerInstance;
import com.intellij.tcserver.server.instance.TcServerModelBase;
import com.intellij.tcserver.server.instance.remote.TcServerRemoteModel;
import com.intellij.tcserver.server.integration.TcServerData;
import com.intellij.tcserver.util.TcServerBundle;
import org.jetbrains.annotations.NonNls;

import java.util.Collection;

public class TcServerDeploymentProvider extends DeploymentProvider {
  @NlsSafe private static final String JMX_DEPLOYMENT_METHOD_NAME = "JMX";
  @NonNls private static final String AVAILABLE_STATUS = "AVAILABLE";
  @NonNls private static final String NEW_AVAILABLE_STATUS = "STARTED";
  @NonNls private static final String NOT_DEPLOYED_STATUS = "NOT_DEPLOYED";
  @NonNls private static final String CONFIGURED_STATUS = "CONFIGURED";
  @NonNls private static final String STOPPED_STATUS = "STOPPED";
  @NonNls private static final String ACTIVATING_STATUS = "STARTING_PREP";
  @NonNls private static final String INITIALIZED_STATUS = "INITIALIZED";

  private static final Logger LOG = Logger.getInstance(TcServerDeploymentProvider.class);

  @SuppressWarnings("UnnecessarilyQualifiedStaticUsage")
  private static final DeploymentMethod JMX_DEPLOYMENT_METHOD =
    new DeploymentMethod(TcServerDeploymentProvider.JMX_DEPLOYMENT_METHOD_NAME, true, false);

  private static final DeploymentMethod[] DEPLOYMENT_METHODS = new DeploymentMethod[]{
    JMX_DEPLOYMENT_METHOD
  };

  @Override
  public void doDeploy(Project project, J2EEServerInstance instance, DeploymentModel model) {
    new Deployer(instance, model).deploy();
  }

  @Override
  public void startUndeploy(J2EEServerInstance activeInstance, DeploymentModel model) {
    new Deployer(activeInstance, model).undeploy();
  }

  @Override
  public SettingsEditor<DeploymentModel> createAdditionalDeploymentSettingsEditor(CommonModel commonModel, DeploymentSource source) {
    return new AdditionalDeploymentSettingsEditor();
  }

  public static JmxProvider getJmxProvider(TcServerInstance instance) {
    TcServerModelBase model = (TcServerModelBase)instance.getCommonModel().getServerModel();
    String host = instance.getCommonModel().getHost();
    final Integer port;

    if (instance.getCommonModel().isLocal()) {
      TcServerData data = (TcServerData)instance.getCommonModel().getApplicationServer().getPersistentData();
      port = data.getJmxPort();
    }
    else {
      port = ((TcServerRemoteModel)model).getJmxPort();
    }

    if (model.isJmxAuthenticationEnabled()) {
      return new JmxProvider(host, port, model.getLogin(), model.getPassword());
    }
    else {
      return new JmxProvider(host, port);
    }
  }

  @Override
  public void updateDeploymentStatus(final J2EEServerInstance instance, final DeploymentModel dModel) {
    final DeploymentContext context = new DeploymentContext(instance, dModel);
    final UpdateDeploymentStatusJob updateDeploymentStatusJob = new UpdateDeploymentStatusJob(context);
    Application application = ApplicationManager.getApplication();
    if (application.isDispatchThread()) {
      application.executeOnPooledThread((Runnable)() -> updateDeploymentStatusJob.execute());
    }
    else {
      updateDeploymentStatusJob.execute();
    }
  }

  @Override
  public DeploymentModel createNewDeploymentModel(CommonModel commonModel, DeploymentSource source) {
    return new TcServerDeploymentModel(commonModel, source);
  }

  @Override
  public Collection<? extends ArtifactType> getSupportedArtifactTypes() {
    return WebArtifactUtil.getInstance().getWebArtifactTypes();
  }

  @Override
  public DeploymentMethod[] getAvailableMethods() {
    return DEPLOYMENT_METHODS;
  }

  private static class Deployer {

    final DeploymentContext myDeploymentContext;

    Deployer(J2EEServerInstance instance, DeploymentModel dModel) {
      myDeploymentContext = new DeploymentContext(instance, dModel);
    }

    public void deploy() {
      LOG.debug("came to deploy Application");
      DeployJob deployJob = new DeployJob(myDeploymentContext);
      putDeploymentChecker(new DeploymentCheckerBase(myDeploymentContext) {

        @Override
        protected boolean onNotDeployed() {
          return deployJob.execute() != DeploymentStatus.ACTIVATING;
        }

        @Override
        protected boolean onDeployed() {
          myDeploymentContext.notifyTextAvailable(TcServerBundle.datedMessage("deploymentProvider.isDeployed", getWebPath()));
          new UndeployJob(myDeploymentContext).execute();
          return false;
        }

        @Override
        protected void onFailed() {
          myDeploymentContext.notifyTextAvailable(TcServerBundle.datedMessage("deploymentProvider.failedDeployment", getWebPath()));
        }
      });
    }

    public void undeploy() {
      LOG.debug("came to undeploy Application");
      putDeploymentChecker(new DeploymentCheckerBase(myDeploymentContext) {

        @Override
        protected boolean onNotDeployed() {
          return true;
        }

        @Override
        protected boolean onDeployed() {
          new UndeployJob(myDeploymentContext).execute();
          return true;
        }

        @Override
        protected void onFailed() {
          myDeploymentContext.notifyTextAvailable(TcServerBundle.datedMessage("deploymentProvider.failedUndeployment", getWebPath()));
        }
      });
    }

    private void putDeploymentChecker(DeploymentCheckerBase deploymentChecker) {
      TcServerInstance serverInstance = myDeploymentContext.getServerInstance();
      ServerInstancePoller poller = serverInstance.getPoller();
      poller.putDeployStateChecker(deploymentChecker);
    }

    private String getWebPath() {
      return myDeploymentContext.getWebPath();
    }
  }

  private static abstract class DeploymentCheckerBase implements DeployStateChecker {

    private final DeploymentContext myDeploymentContext;

    private DeploymentCheckerBase(DeploymentContext context) {
      myDeploymentContext = context;
    }

    @Override
    public DeploymentModel getDeploymentModel() {
      return myDeploymentContext.getDeploymentModel();
    }

    @Override
    public boolean check() {
      LOG.debug("updating deployment status");
      DeploymentStatus deploymentStatus = new UpdateDeploymentStatusJob(myDeploymentContext).execute();
      LOG.debug("deploymentStatus " + deploymentStatus);

      if (deploymentStatus == DeploymentStatus.DEPLOYED || deploymentStatus == DeploymentStatus.PREPARED) {
        return onDeployed();
      }
      else if (deploymentStatus == DeploymentStatus.NOT_DEPLOYED) {
        return onNotDeployed();
      }
      else if (deploymentStatus == DeploymentStatus.ACTIVATING) {
        return false;
      }
      else if (deploymentStatus == DeploymentStatus.DISCONNECTED) {
        return true;
      }

      onFailed();
      return true;
    }

    protected abstract boolean onNotDeployed();

    protected abstract boolean onDeployed();

    protected abstract void onFailed();
  }

  private static class UpdateDeploymentStatusJob extends DeploymentJob {

    UpdateDeploymentStatusJob(DeploymentContext context) {
      super(context);
    }

    @Override
    protected DeploymentStatus doDeploymentJob() throws FailedToInvokeJmxException, FailedToConnectJmxException,
                                                        NotAllowedToConnectException {
      LOG.debug("started getting deployment status");
      String result = getJmxProvider().getApplicationState(getDeploymentContext().getDeploymentModel());
      LOG.debug(" deployment status " + result);

      DeploymentStatus resultStatus;
      if (AVAILABLE_STATUS.equals(result) || NEW_AVAILABLE_STATUS.equals(result)) {
        resultStatus = DeploymentStatus.DEPLOYED;
      }
      else if (NOT_DEPLOYED_STATUS.equals(result)) {
        resultStatus = DeploymentStatus.NOT_DEPLOYED;
      }
      else if (STOPPED_STATUS.equals(result) || CONFIGURED_STATUS.equals(result)) {
        resultStatus = DeploymentStatus.PREPARED;
      }
      else if (ACTIVATING_STATUS.equals(result) || INITIALIZED_STATUS.equals(result)) {
        resultStatus = DeploymentStatus.ACTIVATING;
      }
      else {
        resultStatus = DeploymentStatus.UNKNOWN;
        String message = "Unknown application state: " + result;
        getDeploymentContext().notifyTextAvailable(message + "\n");
        LOG.debug(message);
      }
      return resultStatus;
    }

    @Override
    protected DeploymentStatus getFailedStatus() {
      return DeploymentStatus.UNKNOWN;
    }

    @Override
    protected String getFailedMessage() {
      return TcServerBundle.datedMessage("deploymentProvider.failedRefresh");
    }
  }

  private static class DeployJob extends DeploymentJob {

    private String myPreparedSourcePath;

    DeployJob(DeploymentContext context) {
      super(context);
    }

    @Override
    protected DeploymentStatus doDeploymentJob() throws FailedToInvokeJmxException, FailedToConnectJmxException,
                                                        NotAllowedToConnectException {
      if (myPreparedSourcePath == null) {
        try {
          LOG.debug("started preparing");
          TcServerModelBase model = getDeploymentContext().getServerModel();
          myPreparedSourcePath = model.prepareDeployment(getDeploymentSource().getFilePath());
          LOG.debug("finished preparing");
        }
        catch (RuntimeConfigurationException e) {
          throw new FailedToInvokeJmxException(TcServerBundle.datedMessage("deploymentProvider.inconsistentDeploymentConfiguration",
                                                                           e.getMessage()));
        }
      }

      LOG.debug("started deployment");
      try {
        getJmxProvider().deployApplication(getDeploymentContext().getDeploymentModel(), myPreparedSourcePath);
      }
      catch (FailedToInvokeJmxException e) {
        String failedMessage = getFailedMessage();
        String exceptionMessage = e.getMessage();
        if (exceptionMessage != null) {
          failedMessage = StringUtil.join(failedMessage.trim(), " ", exceptionMessage.trim(), "\n");
        }
        getDeploymentContext().notifyTextAvailable(failedMessage);
        if (StringUtil.contains(failedMessage, "Exception invoking method deployApplication")) {
          getDeploymentContext().notifyTextAvailable("automatic redeploy application\n");
          return DeploymentStatus.ACTIVATING;
        }
        else {
          throw e;
        }
      }
      LOG.debug("finished deployment");
      return DeploymentStatus.DEPLOYED;
    }

    @Override
    protected String getFailedMessage() {
      return TcServerBundle.datedMessage("deploymentProvider.failedDeployment");
    }

    @Override
    protected String getStartMessage() {
      return TcServerBundle.datedMessage("deploymentProvider.startingDeployment", getDeploymentSource().getPresentableName(), getWebPath());
    }

    @Override
    protected String getFinishMessage() {
      return TcServerBundle.datedMessage("deploymentProvider.finishDeployment");
    }

    private DeploymentSource getDeploymentSource() {
      return getDeploymentContext().getDeploymentModel().getDeploymentSource();
    }
  }

  private static class UndeployJob extends DeploymentJob {

    UndeployJob(DeploymentContext context) {
      super(context);
    }

    @Override
    protected DeploymentStatus doDeploymentJob() throws FailedToInvokeJmxException, FailedToConnectJmxException,
                                                        NotAllowedToConnectException {
      LOG.debug("started undeployment");
      getJmxProvider().undeployApplication(getDeploymentContext().getDeploymentModel());
      LOG.debug("finished undeployment");
      return DeploymentStatus.NOT_DEPLOYED;
    }

    @Override
    protected String getFailedMessage() {
      return TcServerBundle.datedMessage("deploymentProvider.failedUndeployment");
    }

    @Override
    protected String getStartMessage() {
      return TcServerBundle.datedMessage("deploymentProvider.startingUndeployment", getWebPath());
    }

    @Override
    protected String getFinishMessage() {
      return TcServerBundle.datedMessage("deploymentProvider.finishUndeployment", getWebPath());
    }
  }

  private static abstract class DeploymentJob {

    private final DeploymentContext myDeploymentContext;

    private final JmxProvider myJmxProvider;

    DeploymentJob(DeploymentContext context) {
      myDeploymentContext = context;
      myJmxProvider = TcServerDeploymentProvider.getJmxProvider(context.getServerInstance());
    }

    public DeploymentStatus execute() {
      DeploymentStatus resultStatus;
      try {
        myDeploymentContext.notifyTextAvailable(getStartMessage());
        resultStatus = doDeploymentJob();
        myDeploymentContext.notifyTextAvailable(getFinishMessage());
      }
      catch (FailedToInvokeJmxException e) {
        resultStatus = getFailedStatus();
        String failedMessage = getFailedMessage();
        String exceptionMessage = e.getMessage();
        if (exceptionMessage != null) {
          failedMessage = StringUtil.join(failedMessage.trim(), " ", exceptionMessage.trim(), "\n");
        }
        myDeploymentContext.notifyTextAvailable(failedMessage);
      }
      catch (FailedToConnectJmxException e) {
        resultStatus = DeploymentStatus.DISCONNECTED;
        myDeploymentContext.notifyTextAvailable(TcServerBundle.datedMessage("deploymentProvider.unableToConnect"));
      }
      catch (NotAllowedToConnectException e) {
        resultStatus = DeploymentStatus.DISCONNECTED;
        myDeploymentContext.notifyTextAvailable(TcServerBundle.datedMessage("deploymentProvider.notAllowedToConnect"));
      }

      myDeploymentContext.setDeploymentStatus(resultStatus);
      return resultStatus;
    }

    protected DeploymentStatus getFailedStatus() {
      return DeploymentStatus.FAILED;
    }

    protected String getFailedMessage() {
      return null;
    }

    protected String getStartMessage() {
      return null;
    }

    protected String getFinishMessage() {
      return null;
    }

    protected DeploymentContext getDeploymentContext() {
      return myDeploymentContext;
    }

    protected JmxProvider getJmxProvider() {
      return myJmxProvider;
    }

    protected String getWebPath() {
      return myDeploymentContext.getWebPath();
    }

    protected abstract DeploymentStatus doDeploymentJob() throws FailedToInvokeJmxException, FailedToConnectJmxException,
                                                                 NotAllowedToConnectException;
  }

  private static class DeploymentContext {

    private final TcServerInstance myServerInstance;

    private final TcServerDeploymentModel myDeploymentModel;

    DeploymentContext(J2EEServerInstance serverInstance, DeploymentModel deploymentModel) {
      myServerInstance = (TcServerInstance)serverInstance;
      myDeploymentModel = (TcServerDeploymentModel)deploymentModel;
    }

    public TcServerInstance getServerInstance() {
      return myServerInstance;
    }

    public TcServerDeploymentModel getDeploymentModel() {
      return myDeploymentModel;
    }

    public TcServerModelBase getServerModel() {
      return (TcServerModelBase)myServerInstance.getCommonModel().getServerModel();
    }

    public String getWebPath() {
      return myDeploymentModel.getWebPath();
    }

    public void setDeploymentStatus(DeploymentStatus status) {
      final CommonModel configuration = myServerInstance.getCommonModel();
      final Project project = configuration.getProject();
      DeploymentManager.getInstance(project).setDeploymentStatus(myDeploymentModel, status, configuration, myServerInstance);
    }

    public void notifyTextAvailable(String text) {
      myServerInstance.notifyTextAvailable(text);
    }
  }
}
