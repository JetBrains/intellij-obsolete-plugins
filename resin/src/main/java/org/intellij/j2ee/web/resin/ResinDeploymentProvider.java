package org.intellij.j2ee.web.resin;

import com.intellij.execution.ExecutionException;
import com.intellij.javaee.appServers.deployment.DeploymentMethod;
import com.intellij.javaee.appServers.deployment.DeploymentModel;
import com.intellij.javaee.appServers.deployment.DeploymentSource;
import com.intellij.javaee.appServers.deployment.DeploymentStatus;
import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.javaee.appServers.serverInstances.J2EEServerInstance;
import com.intellij.javaee.util.DeployStateChecker;
import com.intellij.javaee.web.artifact.WebArtifactUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.packaging.artifacts.ArtifactType;
import org.intellij.j2ee.web.resin.resin.ResinConfiguration;
import org.intellij.j2ee.web.resin.resin.WebApp;
import org.intellij.j2ee.web.resin.resin.common.DeploymentProviderEx;
import org.intellij.j2ee.web.resin.resin.configuration.JmxConfigurationStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;

public class ResinDeploymentProvider extends DeploymentProviderEx {

  private static final Logger LOG = Logger.getInstance(ResinDeploymentProvider.class);

  private static final ResinDeploymentMethod JMX_DEPLOYMENT_METHOD = new ResinDeploymentMethod(
    ResinBundle.message("ResinDeploymentProvider.deploy.method.jmx.name"), true, true) {

    @Override
    public boolean isApplicable(@NotNull CommonModel commonModel) {
      return super.isApplicable(commonModel) && ((ResinModelBase<?>)commonModel.getServerModel()).hasJmxStrategy();
    }

    @Override
    public void doDeploy(Project project, final J2EEServerInstance instance, final DeploymentModel deploymentModel) {
      final JmxConfigurationStrategy strategy = getJmxStrategy(deploymentModel);
      final ResinModelBase serverModel = (ResinModelBase)deploymentModel.getServerModel();
      final WebApp webApp = getWebApp(deploymentModel);
      boolean success =
        strategy != null && strategy.deployWithJmx(serverModel, webApp);
      if (success) {
        setDeploymentStatus(instance, deploymentModel, DeploymentStatus.UNKNOWN);
        ((ResinServerInstance)instance).getPoller().putDeployStateChecker(new DeployStateChecker() {
          @Override
          public DeploymentModel getDeploymentModel() {
            return deploymentModel;
          }

          @Override
          public boolean check() {
            Ref<Boolean> isFinal = new Ref<>(false);
            setDeploymentStatus(instance, deploymentModel, strategy.getDeployStateWithJmx(serverModel, webApp, isFinal));
            return isFinal.get();
          }
        });
      }
      else {
        setDeploymentStatus(instance, deploymentModel, DeploymentStatus.FAILED);
      }
    }

    @Override
    public void startUndeploy(J2EEServerInstance instance, DeploymentModel deploymentModel) {
      ((ResinServerInstance)instance).getPoller().removeDeployStateChecker(deploymentModel);
      JmxConfigurationStrategy strategy = getJmxStrategy(deploymentModel);
      boolean success =
        strategy != null && strategy.undeployWithJmx((ResinModelBase)deploymentModel.getServerModel(), getWebApp(deploymentModel));
      setDeploymentStatus(instance, deploymentModel, success ? DeploymentStatus.NOT_DEPLOYED : DeploymentStatus.UNKNOWN);
    }

    @Nullable
    private JmxConfigurationStrategy getJmxStrategy(DeploymentModel deploymentModel) {
      return ((ResinModelBase<?>)deploymentModel.getServerModel()).getJmxStrategy();
    }
  };

  public static final ResinDeploymentMethod CONF_DEPLOYMENT_METHOD = new ResinDeploymentMethod(
    ResinBundle.message("ResinDeploymentProvider.deploy.method.conf.name"), true, false) {

    @Override
    public void doDeploy(Project project, J2EEServerInstance instance, DeploymentModel deploymentModel) {
      setDeploymentStatus(instance, deploymentModel, DeploymentStatus.DEPLOYED);
    }

    @Override
    public void startUndeploy(J2EEServerInstance instance, DeploymentModel deploymentModel) {
      boolean success = false;
      try {
        ResinModel resinModel = (ResinModel)deploymentModel.getServerModel();
        ResinConfiguration resinConfiguration = resinModel.getOrCreateResinConfiguration(false);
        WebApp webApp = getWebApp(deploymentModel);
        success = webApp != null && resinConfiguration.undeploy(webApp);
      }
      catch (ExecutionException e) {
        LOG.error(e);
      }
      setDeploymentStatus(instance, deploymentModel, success ? DeploymentStatus.NOT_DEPLOYED : DeploymentStatus.UNKNOWN);
    }
  };

  private static final ResinDeploymentMethod DEFAULT_DEPLOYMENT_METHOD = JMX_DEPLOYMENT_METHOD;

  private static final DeploymentMethod[] DEPLOYMENT_METHODS = new DeploymentMethod[]{ //
    JMX_DEPLOYMENT_METHOD, CONF_DEPLOYMENT_METHOD //
  };

  private static ResinDeploymentMethod getDeploymentMethod(DeploymentModel deploymentModel) {
    DeploymentMethod method = deploymentModel.getDeploymentMethod();
    return !(method instanceof ResinDeploymentMethod) ? DEFAULT_DEPLOYMENT_METHOD : (ResinDeploymentMethod)method;
  }

  @Override
  public void doDeploy(Project project, J2EEServerInstance instance, DeploymentModel deploymentModel) {
    getDeploymentMethod(deploymentModel).doDeploy(project, instance, deploymentModel);
  }

  @Override
  public DeploymentModel createNewDeploymentModel(CommonModel commonModel, DeploymentSource source) {
    return new ResinModuleDeploymentModel(commonModel, source);
  }

  @Override
  public SettingsEditor<DeploymentModel> createAdditionalDeploymentSettingsEditor(CommonModel commonModel, DeploymentSource source) {
    ResinModelBase resinModel = ((ResinModelBase)commonModel.getServerModel());
    return resinModel.createAdditionalDeploymentSettingsEditor(commonModel, source);
  }

  @Override
  public Collection<? extends ArtifactType> getSupportedArtifactTypes() {
    return Arrays.asList(WebArtifactUtil.getInstance().getExplodedWarArtifactType(), WebArtifactUtil.getInstance().getWarArtifactType());
  }

  @Override
  public void startUndeploy(J2EEServerInstance instance, DeploymentModel deploymentModel) {
    getDeploymentMethod(deploymentModel).startUndeploy(instance, deploymentModel);
  }

  @Override
  public void updateDeploymentStatus(J2EEServerInstance j2EEServerInstance, DeploymentModel deploymentModel) {
  }

  @Nullable
  public static WebApp getWebApp(DeploymentModel deploymentModel) {
    ResinModuleDeploymentModel resinModel = (ResinModuleDeploymentModel)deploymentModel;
    String filePath = deploymentModel.getDeploymentSource().getFilePath();
    return filePath == null
           ? null
           : new WebApp(resinModel.isDefaultContextPath(), resinModel.getContextPath(), resinModel.getHost(), filePath,
                        ((ResinModelBase<?>)deploymentModel.getServerModel()).getCharset());
  }

  @Override
  public DeploymentMethod[] getAvailableMethods() {
    return DEPLOYMENT_METHODS;
  }

  private static abstract class ResinDeploymentMethod extends DeploymentMethod {

    ResinDeploymentMethod(String name, boolean local, boolean remote) {
      super(name, local, remote);
    }

    public abstract void doDeploy(Project project, J2EEServerInstance instance, DeploymentModel deploymentModel);

    public abstract void startUndeploy(J2EEServerInstance instance, DeploymentModel deploymentModel);
  }
}
