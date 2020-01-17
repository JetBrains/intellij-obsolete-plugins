package com.intellij.javaee.heroku.agent.cloud;

import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import com.heroku.api.Proc;
import com.heroku.api.exception.HerokuAPIException;
import com.heroku.api.request.log.Log;
import com.heroku.sdk.deploy.DeployWar;
import com.heroku.sdk.deploy.utils.Main;
import com.intellij.javaee.heroku.agent.HerokuApplicationImpl;
import com.intellij.remoteServer.agent.util.CloudAgentLogger;
import com.intellij.remoteServer.agent.util.CloudAgentLoggingHandler;
import com.intellij.remoteServer.agent.util.CloudGitApplication;
import com.intellij.remoteServer.agent.util.log.*;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * @author michael.golubev
 */
public class HerokuCloudAgentDeploymentImpl implements HerokuCloudAgentDeployment {

  private static final String LOG_PIPE_NAME = "Log";
  private static final String PROCESS_TYPE = "web";
  private static final String JAVA_OPTS = "JAVA_OPTS";
  private static final String DEBUG_OPTION = "-Xdebug";
  private static final String RUN_JDWP_OPTION = "-Xrunjdwp:transport=dt_socket,address=";
  private static final String WEBAPP_RUNNER_URL =
          "http://central.maven.org/maven2/com/github/jsimone/webapp-runner/8.5.23.0/webapp-runner-8.5.23.0.jar";
  private static final String APP_NAME_PROPERTY = "heroku.appName";

  private final HerokuApiTaskProvider myTaskProvider;
  private final String myDeploymentName;
  private final CloudAgentLoggingHandler myDefaultLoggingHandler;
  private final LogAgentManager myLogManager;
  private final CloudAgentLogger myLogger;
  private final String myApiKey;
  private Integer myInitialDynoAmount;

  public HerokuCloudAgentDeploymentImpl(HerokuApiTaskProvider taskProvider,
                                        String deploymentName,
                                        CloudAgentLoggingHandler loggingHandler,
                                        LogAgentManager logManager,
                                        CloudAgentLogger logger,
                                        String apiKey) {
    myTaskProvider = taskProvider;
    myDeploymentName = deploymentName;
    myDefaultLoggingHandler = loggingHandler;
    myLogManager = logManager;
    myLogger = logger;
    myApiKey = apiKey;
  }

  @Override
  public CloudGitApplication createApplication() {
    return myTaskProvider.new ApiTask<CloudGitApplication>() {

      @Override
      protected CloudGitApplication doPerform(HerokuAPI api) throws HerokuAPIException {
        return doCreateApp(api);
      }
    }.perform();
  }

  private CloudGitApplication doCreateApp(HerokuAPI api) {
    return new HerokuApplicationImpl(api.createApp(new App().named(myDeploymentName)));
  }

  @Override
  public CloudGitApplication findApplication() {
    return myTaskProvider.new ApiTask<CloudGitApplication>() {

      @Override
      protected CloudGitApplication doPerform(HerokuAPI api) throws HerokuAPIException {
        return doFindApp(api);
      }
    }.perform();
  }

  private CloudGitApplication doFindApp(HerokuAPI api) {
    for (App app : api.listApps()) {
      if (app.getName().equals(myDeploymentName)) {
        return new HerokuApplicationImpl(app);
      }
    }
    return null;
  }

  @Override
  public void deleteApplication() {
    myTaskProvider.new ApiTask() {

      @Override
      protected Object doPerform(HerokuAPI api) throws HerokuAPIException {
        destroyApp(api);
        return null;
      }
    }.perform();
  }

  @Override
  public CompletableFuture<LogListener> startListeningLog(final CloudAgentLoggingHandler loggingHandler) {
    CompletableFuture<LogListener> result = new CompletableFuture<>();
    myLogManager.startListeningLog(myDeploymentName, new LogPipeProvider() {

      @Override
      public List<? extends LogPipe> createLogPipes(String deploymentName) {
        return Collections.singletonList(new HerokuLogPipe(deploymentName, loggingHandler, result));
      }
    });
    return result;
  }

  @Override
  public void stopListeningLog() {
    myLogManager.stopListeningLog(myDeploymentName);
  }

  private void destroyApp(HerokuAPI api) {
    api.destroyApp(myDeploymentName);
  }

  @Override
  public void deployWar(final File file) {
    myTaskProvider.new ApiTask() {

      @Override
      protected Object doPerform(HerokuAPI api) throws HerokuAPIException {
        if (doFindApp(api) == null) {
          doCreateApp(api);
        }

        try {
          System.setProperty(APP_NAME_PROPERTY, myDeploymentName);
          Main.deploy((appName, buildpacks) -> new DeployWar(appName, file, new URL(WEBAPP_RUNNER_URL), buildpacks) {
            @Override
            public void logInfo(String message) {
              myDefaultLoggingHandler.println(message);
            }
          });
        }
        catch (Exception e) {
          myLogger.debug(e.getMessage());
        }
        finally {
          System.clearProperty(APP_NAME_PROPERTY);
        }
        return null;
      }
    }.perform();
  }

  @Override
  public void attachDebugRemote(final String host, final Integer port) {
    myTaskProvider.new ApiTask() {

      @Override
      protected Object doPerform(HerokuAPI api) throws HerokuAPIException {
        App app = api.getApp(myDeploymentName);
        int appDynos = api.listDynos(app.getName()).size();
        if (appDynos > 1) {
          myInitialDynoAmount = appDynos;
          api.scale(myDeploymentName, PROCESS_TYPE, 1);
        }
        else {
          myInitialDynoAmount = null;
        }

        Map<String, String> appConfig = api.listConfig(myDeploymentName);
        String javaOpts = appConfig.get(JAVA_OPTS);

        Map<String, String> javaOptsConfig = new HashMap<>();
        javaOptsConfig.put(JAVA_OPTS, notNullize(javaOpts) + " " + DEBUG_OPTION + " " + RUN_JDWP_OPTION + host + ":" + port);
        api.updateConfig(myDeploymentName, javaOptsConfig);
        return null;
      }
    }.perform();
  }

  @Override
  public void detachDebugRemote() {
    myTaskProvider.new ApiTask() {

      @Override
      protected Object doPerform(HerokuAPI api) throws HerokuAPIException {
        if (doFindApp(api) == null) {
          return null;
        }
        boolean needRestart = false;
        if (myInitialDynoAmount != null) {
          api.scale(myDeploymentName, PROCESS_TYPE, myInitialDynoAmount);
          needRestart = true;
        }
        Map<String, String> appConfig = api.listConfig(myDeploymentName);
        StringBuilder optsBuilder = new StringBuilder();
        String javaOpts = appConfig.get(JAVA_OPTS);
        if (javaOpts != null) {
          String[] opts = javaOpts.split("\\s+");
          for (String opt : opts) {
            if (opt.equals(DEBUG_OPTION) || opt.startsWith(RUN_JDWP_OPTION)) {
              needRestart = true;
            }
            else {
              if (optsBuilder.length() > 0) {
                optsBuilder.append(" ");
              }
              optsBuilder.append(opt);
            }
          }
          Map<String, String> javaOptsConfig = new HashMap<>();
          javaOptsConfig.put(JAVA_OPTS, optsBuilder.toString());
          api.updateConfig(myDeploymentName, javaOptsConfig);
        }
        if (needRestart) {
          api.restartDynos(myDeploymentName);
        }
        return null;
      }
    }.perform();
  }

  @Override
  public void startBashSession() {
    myTaskProvider.new ApiTask<Object>() {

      @Override
      protected Object doPerform(HerokuAPI api) throws HerokuAPIException, IOException {
        final HerokuBashPipe bashPipe = new HerokuBashPipe(api);
        Proc proc = bashPipe.getProc();

        myLogManager.startOrContinueListeningLog(proc.getProcess(), new LogPipeProvider() {

          @Override
          public List<? extends LogPipeBase> createLogPipes(String deploymentName) {
            return Collections.singletonList(bashPipe);
          }
        });
        return null;
      }
    }.perform();
  }

  @Override
  public CloudGitApplication findApplication4Repository(final String[] repositoryUrls) {
    return myTaskProvider.new ApiTask<CloudGitApplication>() {

      @Override
      protected CloudGitApplication doPerform(HerokuAPI api) throws HerokuAPIException {
        Set<String> urlsSet = new HashSet<>(Arrays.asList(repositoryUrls));
        for (App app : api.listApps()) {
          if (urlsSet.contains(app.getGitUrl())) {
            return new HerokuApplicationImpl(app);
          }
        }
        return null;
      }
    }.perform();
  }

  private static String notNullize(String text) {
    return text == null ? "" : text;
  }

  private class HerokuLogPipe extends LogPipe {
    private final CompletableFuture<LogListener> myStartListeningCallback;

    HerokuLogPipe(String deploymentName, CloudAgentLoggingHandler loggingHandler, CompletableFuture<LogListener> startListeningCallback) {
      super(deploymentName, LOG_PIPE_NAME, myLogger, loggingHandler);
      myStartListeningCallback = startListeningCallback;
    }

    @Override
    protected InputStream createInputStream(final String deploymentName) {
      return myTaskProvider.new ApiSilentTask<InputStream>() {

        @Override
        protected InputStream doPerform(HerokuAPI api) throws HerokuAPIException {
          return api.getLogs(new Log.LogRequestBuilder().app(deploymentName).tail(true)).openStream();
        }
      }.perform();
    }

    @Override
    protected void onStartListening(LogListener logListener) {
      myStartListeningCallback.complete(logListener);
    }
  }

  private class HerokuBashPipe extends TerminalPipe {

    private final Proc myProc;
    private final OutputStream myOutputStream;
    private final InputStream myInputStream;

    HerokuBashPipe(HerokuAPI api) throws HerokuAPIException, IOException {
      super("Heroku Bash", myDefaultLoggingHandler);
      // FIXME: IDEA-175174: not clear how to runAttached with new v3 API
      // FIXME: before v3 code: myProc = api.runAttached(myDeploymentName, "bash").getProc();
      myProc = new Proc();

      String host = myProc.getRendezvousUrl().getHost();
      int port = myProc.getRendezvousUrl().getPort();
      String secret = myProc.getRendezvousUrl().getPath().substring(1);
      SocketFactory socketFactory = SSLSocketFactory.getDefault();
      final Socket socket = socketFactory.createSocket(host, port);

      myInputStream = socket.getInputStream();
      myOutputStream = socket.getOutputStream();

      OutputStreamWriter outputStreamWriter = new OutputStreamWriter(myOutputStream, StandardCharsets.UTF_8);
      //noinspection IOResourceOpenedButNotSafelyClosed
      BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

      InputStreamReader inputStreamReader = new InputStreamReader(myInputStream, StandardCharsets.UTF_8);
      //noinspection IOResourceOpenedButNotSafelyClosed
      BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

      bufferedWriter.write(secret);
      bufferedWriter.flush();

      bufferedReader.readLine();

      bufferedWriter.write("\n");
      bufferedWriter.flush();
    }

    public Proc getProc() {
      return myProc;
    }

    @Override
    public OutputStream getOutputStream() {
      return myOutputStream;
    }

    @Override
    public InputStream getInputStream() {
      return myInputStream;
    }

    @Override
    public void close() {
      super.close();
      try {
        if (myOutputStream != null) {
          myOutputStream.close();
        }
        if (myInputStream != null) {
          myInputStream.close();
        }
      }
      catch (IOException e) {
        myLogger.debugEx(e);
      }
    }
  }
}
