package com.intellij.dmserver.deploy;

import com.intellij.dmserver.artifacts.DMBundleArtifactType;
import com.intellij.dmserver.artifacts.DMConfigArtifactType;
import com.intellij.dmserver.common.DeploymentProviderEx;
import com.intellij.dmserver.deploy.jmx.ConnectorListBundlesCommand;
import com.intellij.dmserver.deploy.jmx.QueryDeploymentStatusCommand;
import com.intellij.dmserver.facet.DMCompositeFacet;
import com.intellij.dmserver.run.DMServerInstance;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.javaee.appServers.deployment.DeploymentMethod;
import com.intellij.javaee.appServers.deployment.DeploymentModel;
import com.intellij.javaee.appServers.deployment.DeploymentSource;
import com.intellij.javaee.appServers.deployment.DeploymentStatus;
import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.javaee.appServers.serverInstances.J2EEServerInstance;
import com.intellij.javaee.web.artifact.WebArtifactUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.util.containers.ContainerUtil;
import com.springsource.server.management.remote.Bundle;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class DMServerDeploymentProvider extends DeploymentProviderEx {
  private static final Logger LOG = Logger.getInstance(DMServerDeploymentProvider.class);

  private static final DeploymentMethod JMX_DEPLOYMENT_METHOD = new DeploymentMethod(
    DmServerBundle.message("DMServerDeploymentProvider.deploy.method.jmx.name"), true, false);

  private static final DeploymentMethod DEFAULT_DEPLOYMENT_METHOD = JMX_DEPLOYMENT_METHOD;

  private static final DeploymentMethod[] DEPLOYMENT_METHODS = new DeploymentMethod[]{ //
    JMX_DEPLOYMENT_METHOD //
  };

  @Override
  public DeploymentModel createNewDeploymentModel(CommonModel commonModel, DeploymentSource source) {
    return new DMServerDeploymentModel(commonModel, source);
  }

  @Override
  public void updateDeploymentStatus(J2EEServerInstance instance, DeploymentModel model) {
    new DeploymentOperation() {

      @Override
      protected DeploymentStatus doExecute(DMServerInstance dmServerInstance, DeploymentModel model)
        throws IOException, ExecutionException, TimeoutException {
        Boolean result = new QueryDeploymentStatusCommand(dmServerInstance, model).execute();
        if (result == null) {
          return DeploymentStatus.UNKNOWN;
        }
        else {
          return result ? DeploymentStatus.DEPLOYED : DeploymentStatus.NOT_DEPLOYED;
        }
      }

      @Override
      protected DeploymentStatus getErrorStatus() {
        return DeploymentStatus.UNKNOWN;
      }
    }.execute(instance, model);
  }

  @Override
  public void doDeploy(Project project, J2EEServerInstance instance, DeploymentModel model) {
    new DeploymentOperation() {

      @Override
      protected DeploymentStatus doExecute(DMServerInstance dmServerInstance, DeploymentModel model)
        throws IOException, ExecutionException, TimeoutException {

        DeploymentMethod method = model.getDeploymentMethod();
        if (method == null) {
          method = DEFAULT_DEPLOYMENT_METHOD;
        }

        boolean success;
        if (method == JMX_DEPLOYMENT_METHOD) {
          List<IDMCommand> deployCommands = new DeployCommandsCollector(dmServerInstance, model).collectCommands();
          success = !deployCommands.isEmpty();
          for (IDMCommand deployCommand : deployCommands) {
            Object deployResult = deployCommand.execute();
            success = deployResult != null;
            if (!success) {
              break;
            }
            if (deployResult instanceof DeploymentIdentity) {
              dmServerInstance.registerDeployment(model, (DeploymentIdentity)deployResult);
            }
          }
        }
        else {
          LOG.error("Unknown deployment method");
          success = false;
        }
        return success ? DeploymentStatus.DEPLOYED : DeploymentStatus.FAILED;
      }

      @Override
      protected DeploymentStatus getErrorStatus() {
        return DeploymentStatus.FAILED;
      }
    }.execute(instance, model);
  }

  @Nullable
  public static Map<Long, Bundle> listAllDeployedBundles(DMServerInstance serverInstance) {
    try {
      return new ConnectorListBundlesCommand(serverInstance).execute();
    }
    catch (Exception e) {
      LOG.error(e);
      return null;
    }
  }

  @Override
  public Collection<? extends ArtifactType> getSupportedArtifactTypes() {
    return ContainerUtil.append(DMCompositeFacet.getSupportedArtifactTypes(),
                                DMBundleArtifactType.getInstance(),
                                DMConfigArtifactType.getInstance(),
                                WebArtifactUtil.getInstance().getWarArtifactType());
  }

  @Override
  public void startUndeploy(J2EEServerInstance activeInstance, DeploymentModel model) {
    new DeploymentOperation() {

      @Override
      protected DeploymentStatus doExecute(DMServerInstance dmServerInstance, DeploymentModel model)
        throws IOException, ExecutionException, TimeoutException {
        List<IDMCommand> undeployCommands = new UndeployCommandsCollector(dmServerInstance, model).collectCommands();
        for (IDMCommand undeployCommand : undeployCommands) {
          undeployCommand.execute();
        }
        return DeploymentStatus.NOT_DEPLOYED;
      }

      @Override
      protected DeploymentStatus getErrorStatus() {
        return DeploymentStatus.UNKNOWN;
      }
    }.execute(activeInstance, model);
  }

  @Override
  public DeploymentMethod[] getAvailableMethods() {
    return DEPLOYMENT_METHODS;
  }

  @Override
  public boolean isDeployOrderMatter() {
    return true;
  }

  private static abstract class DeploymentOperation {

    public void execute(J2EEServerInstance serverInstance, DeploymentModel model) {
      try {
        DeploymentStatus status = doExecute((DMServerInstance)serverInstance, model);
        setDeploymentStatus(serverInstance, model, status);
      }
      catch (IOException | TimeoutException | ExecutionException e) {
        LOG.debug(e);
        setErrorStatus(serverInstance, model);
      }
    }

    private void setErrorStatus(J2EEServerInstance serverInstance, DeploymentModel model) {
      setDeploymentStatus(serverInstance, model, getErrorStatus());
    }

    protected abstract DeploymentStatus doExecute(DMServerInstance dmServerInstance, DeploymentModel model)
      throws IOException, ExecutionException, TimeoutException;

    protected abstract DeploymentStatus getErrorStatus();
  }
}
