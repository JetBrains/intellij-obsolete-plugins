package com.intellij.dmserver.test;

import com.intellij.JavaTestUtil;
import com.intellij.dmserver.artifacts.DMBundleArtifactType;
import com.intellij.dmserver.integration.DMServerIntegration;
import com.intellij.dmserver.integration.DMServerIntegrationData;
import com.intellij.dmserver.run.DMServerRunConfigurationType;
import com.intellij.execution.*;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.javaee.appServers.appServerIntegrations.ApplicationServer;
import com.intellij.javaee.appServers.deployment.DeploymentModel;
import com.intellij.javaee.appServers.deployment.DeploymentProvider;
import com.intellij.javaee.appServers.deployment.DeploymentSettings;
import com.intellij.javaee.appServers.ex.openapi.ex.DeploymentManagerEx;
import com.intellij.javaee.appServers.run.configuration.CommonStrategy;
import com.intellij.javaee.appServers.serverInstances.ApplicationServersManager;
import com.intellij.javaee.appServers.serverInstances.J2EEServerInstance;
import com.intellij.javaee.appServers.serverInstances.RunAppServerInstanceManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.impl.compiler.ArtifactCompileScope;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.util.SystemProperties;
import com.intellij.util.concurrency.Semaphore;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author michael.golubev
 */
public class DMDeploymentTest extends DMTestBase {

  private Set<ProcessHandler> myServerProcessHandlers;

  @Override
  protected void initApplication() throws Exception {
    super.initApplication();
    JavaTestUtil.setupInternalJdkAsTestJDK(getTestRootDisposable(), "JDK");
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myServerProcessHandlers = new HashSet<>();
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      for (ProcessHandler processHandler : myServerProcessHandlers) {
        processHandler.destroyProcess();
      }
      myServerProcessHandlers.clear();
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      super.tearDown();
    }
  }

  public void testBundleDeploy() throws Throwable {
    if (!SystemProperties.getUserName().contains("golubev")) {
      return;
    }

    @NonNls final String MODULE_NAME = "mbundle1";
    final int executionStartTimeout = 5 * 1000;
    final int executionStopTimeout = 20 * 1000;
    @NonNls final String SERVER_PATH = "C:\\springsource-dm-server-2.0.0.RC1";

    JavaTestUtil.setupInternalJdkAsTestJDK(getTestRootDisposable(), "JDK");

    setupProjectOutput();

    final Module bundleModule = initBundleModule(MODULE_NAME);

    WriteAction.runAndWait(() -> {
      ContentEntry contentEntry = ModuleRootManager.getInstance(bundleModule).getContentEntries()[0];
      VirtualFile sourceDir = contentEntry.getSourceFolderFiles()[0];
      VirtualFile classFile = sourceDir.createChildDirectory(this, "p").createChildData(this, "c.java");
      setFileText(classFile, """
        package p;

        public class c10 {
        }""");
    });
    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();

    ConfigurationFactory[] configurationFactories = DMServerRunConfigurationType.getInstance().getConfigurationFactories();
    assertTrue("at least one factory is expected", configurationFactories.length > 0);
    ConfigurationFactory localConfigurationFactory = configurationFactories[0];

    RunnerAndConfigurationSettings runConfigurationSettings =
      RunManager.getInstance(getProject()).createConfiguration("DM Server Local Run Config", localConfigurationFactory);

    RunConfiguration runConfiguration = runConfigurationSettings.getConfiguration();
    assertInstanceOf(runConfiguration, CommonStrategy.class);

    final CommonStrategy commonStrategy = (CommonStrategy)runConfiguration;

    ApplicationServer applicationServer = ApplicationServersManager.getInstance().createServer(DMServerIntegration.getInstance(),
                                                                                               new DMServerIntegrationData(SERVER_PATH));

    commonStrategy.setApplicationServer(applicationServer);

    DeploymentSettings deploymentSettings = commonStrategy.getDeploymentSettings();
    assertNotNull(deploymentSettings);

    Collection<? extends Artifact> bundleArtifacts =
      ArtifactManager.getInstance(getProject()).getArtifactsByType(DMBundleArtifactType.getInstance());
    Artifact bundleArtifact = assertOneElement(bundleArtifacts);

    deploymentSettings.getOrCreateModel(bundleArtifact);

    final Semaphore executionStartSemaphore = new Semaphore();
    executionStartSemaphore.down();

    final Semaphore executionStopSemaphore = new Semaphore();

    final Ref<Boolean> deploySuccessRef = new Ref<>(false);

    MessageBusConnection connection = getProject().getMessageBus().connect();
    connection.subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {
      @Override
      public void processStarting(@NotNull String executorId, @NotNull ExecutionEnvironment env) {
        if (env.getRunProfile() != commonStrategy) {
          return;
        }
        executionStopSemaphore.down();
        executionStartSemaphore.up();
      }

      @Override
      public void processNotStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env) {
        if (env.getRunProfile() != commonStrategy) {
          return;
        }
        executionStopSemaphore.up();
      }

      @Override
      public void processStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env, final @NotNull ProcessHandler handler) {
        if (env.getRunProfile() != commonStrategy) {
          return;
        }

        myServerProcessHandlers.add(handler);

        final List<Artifact> artifacts = new ArrayList<>();
        for (DeploymentModel artifactDeploymentModel : commonStrategy.getDeploymentModels()) {
          artifacts.add(artifactDeploymentModel.getArtifact());
        }
        assertOneElement(artifacts);

        CompilerManager.getInstance(getProject())
                       .make(ArtifactCompileScope.createArtifactsScope(getProject(), artifacts), new CompileStatusNotification() {

                         @Override
                         public void finished(boolean aborted, int errors, int warnings, @NotNull final CompileContext compileContext) {
                           assertFalse("Compilation aborted", aborted);
                           assertEquals(0, errors);

                           DeploymentProvider provider = DeploymentManagerEx.getProvider(commonStrategy);
                           assertNotNull(provider);

                           J2EEServerInstance serverInstance =
                             RunAppServerInstanceManager.getInstance(getProject()).findInstance(commonStrategy);
                           assertNotNull(serverInstance);

                           for (DeploymentModel artifactDeploymentModel : commonStrategy.getDeploymentModels()) {
                             provider.doDeploy(getProject(), serverInstance, artifactDeploymentModel);
                           }

                           deploySuccessRef.set(true);
                           myServerProcessHandlers.remove(handler);
                           handler.destroyProcess();
                         }
                       });
      }

      @Override
      public void processTerminated(@NotNull String executorId,
                                    @NotNull ExecutionEnvironment env,
                                    @NotNull ProcessHandler handler,
                                    int exitCode) {
        if (env.getRunProfile() != commonStrategy) {
          return;
        }
        executionStopSemaphore.up();
      }
    });

    Executor executor = DefaultRunExecutor.getRunExecutorInstance();

    assertTrue("Run configuration can't be run", RunManagerImpl.canRunConfiguration(runConfigurationSettings, executor));

    ProgramRunnerUtil.executeConfiguration(runConfigurationSettings, executor);

    if (!executionStartSemaphore.waitFor(executionStartTimeout)) {
      fail("Execution isn't started after " + executionStartTimeout + " msec");
    }
    else if (!executionStopSemaphore.waitFor(executionStopTimeout)) {
      fail("Execution isn't stopped after " + executionStopTimeout + " msec");
    }
    else {
      assertTrue("Deploy isn't successful", deploySuccessRef.get());
    }
  }
}
