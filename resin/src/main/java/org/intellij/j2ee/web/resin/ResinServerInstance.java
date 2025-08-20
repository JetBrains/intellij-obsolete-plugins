package org.intellij.j2ee.web.resin;

import com.intellij.debugger.DebuggerManager;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.engine.DebugProcessListener;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.javaee.appServers.serverInstances.DefaultJ2EEServerEvent;
import com.intellij.javaee.appServers.serverInstances.DefaultServerInstance;
import com.intellij.javaee.util.ServerInstancePoller;
import com.intellij.javaee.web.debugger.engine.DefaultJSPPositionManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import org.intellij.j2ee.web.resin.resin.ResinConfiguration;
import org.intellij.j2ee.web.resin.resin.ResinInstallation;
import org.intellij.j2ee.web.resin.resin.version.ResinVersion;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

public class ResinServerInstance extends DefaultServerInstance {

  private static final Logger LOG = Logger.getInstance(ResinServerInstance.class);

  private final ServerInstancePoller myPoller = new ServerInstancePoller();

  public ResinServerInstance(CommonModel runConfiguration) {
    super(runConfiguration);
  }

  public ServerInstancePoller getPoller() {
    return myPoller;
  }

  @Override
  public void start(final ProcessHandler processHandler) {
    super.start(processHandler);
    fireServerListeners(new DefaultJ2EEServerEvent(true, false));

    final ResinModelBase resinModel = (ResinModelBase)getServerModel();
    DebuggerManager.getInstance(resinModel.getProject()).addDebugProcessListener(processHandler, new DebugProcessListener() {
      @Override
      public void processAttached(@NotNull final DebugProcess process) {
        if (resinModel instanceof ResinModel) {
          try {
            ResinModel resinLocalModel = (ResinModel)resinModel;

            if (resinLocalModel.isDebugConfiguration()) {
              final ResinConfiguration configuration = resinLocalModel.getOrCreateResinConfiguration(false);
              File configFile = configuration.getConfigFile();
              StringWriter sw = new StringWriter();
              sw.append("\n---\n");
              sw.append(ResinBundle.message("message.text.resin.conf.debug", configFile.getAbsolutePath()));
              sw.append("\n");
              sw.append(FileUtil.loadFile(configFile));
              sw.append("\n---\n");
              processHandler.notifyTextAvailable(sw.toString(), ProcessOutputTypes.SYSTEM);
            }
          }
          catch (ExecutionException | IOException e) {
            LOG.error(e);
          }
        }

        ResinInstallation installation = resinModel.getInstallation();

        if (installation != null && installation.getVersion() != ResinVersion.VERSION_2_X) {
          //TODO SCF compiler bug ?
          process.appendPositionManager(new DefaultJSPPositionManager(process, getScopeFacets(getCommonModel())) {
            @Override
            protected String getGeneratedClassesPackage() {
              return "_jsp";
            }
          });
        }
      }
    });

    myPoller.onInstanceStart();
  }

  @Override
  public void shutdown() {
    myPoller.onInstanceShutdown();
    super.shutdown();
    ProcessHandler processHandler = getProcessHandler();
    if (processHandler instanceof OSProcessHandler) ((OSProcessHandler)processHandler).getProcess().destroy();
  }
}
