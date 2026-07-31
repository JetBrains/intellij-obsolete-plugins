/*
 * Copyright 2000-2006 JetBrains s.r.o.
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
package com.intellij.gwt.run;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputType;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.util.JavaParametersUtil;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.make.GwtCompilerPaths;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.rpc.RemoteServiceUtil;
import com.intellij.gwt.run.remoteUi.GwtErrorFilter;
import com.intellij.gwt.run.remoteUi.RemoteUiConnection;
import com.intellij.gwt.run.remoteUi.RemoteUiView;
import com.intellij.gwt.sdk.GwtDependenciesStorage;
import com.intellij.gwt.sdk.GwtSdk;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.browsers.WebBrowserManager;
import com.intellij.jarRepository.JarRepositoryManager;
import com.intellij.javaee.artifact.JavaeeArtifactUtil;
import com.intellij.javaee.web.WebRoot;
import com.intellij.javaee.web.artifact.WebArtifactUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationActivationListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.PluginPathManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.libraries.ui.OrderRoot;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.ObjectUtils;
import com.intellij.util.PathUtil;
import com.intellij.util.PathsList;
import com.intellij.util.SystemProperties;
import com.intellij.util.containers.ContainerUtil;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.aether.ArtifactKind;
import org.jetbrains.jps.gwt.build.common.DelegatingOutputLineReader;
import org.jetbrains.jps.gwt.build.common.OutputLineReader;
import org.jetbrains.jps.gwt.model.GwtDependenciesResolver;
import org.jetbrains.jps.gwt.model.MavenCoordinates;
import org.jetbrains.jps.gwt.model.impl.sdk.JpsGwtDependenciesStorage;
import org.jetbrains.jps.model.library.JpsMavenRepositoryLibraryDescriptor;
import org.jetbrains.jps.model.serialization.PathMacroUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.util.Collections.emptyList;

public final class GwtCommandLineState extends JavaCommandLineStateEx {
  public static final Key<HostedModeWarDirectoryGenerator> GWT_GENERATOR_KEY = Key.create("GWT_GENERATOR");
  public static final Key<GwtCommandLineState> GWT_CONFIGURATION_STATE_KEY = Key.create("GWT_CONFIGURATION_STATE");
  private static final Logger LOG = Logger.getInstance(GwtCommandLineState.class);
  private static final @NonNls String WEB_XML_PATH = "com/google/gwt/dev/etc/tomcat/webapps/ROOT/WEB-INF/web.xml";
  private static final @NonNls String APP_ENGINE_GENERATED_DIR = "WEB-INF/appengine-generated";
  public static final FilenameFilter GWT_TEMP_FILES_FILTER = (dir, name) -> name.startsWith("gwt") && name.endsWith("byte-cache");
  private static final boolean SHOW_SHELL_WINDOW_FOR_DEV_MODE =
    SystemProperties.getBooleanProperty("idea.gwt.show.shell.window.for.dev.mode", false);

  private final GwtFacet myFacet;
  private final Module myModule;
  private final Module myFacetModule;
  private final Project myProject;
  private final List<String> myGwtModules;
  private final Executor myExecutor;
  private final GwtDevModeServer myServer;
  private final String myRunConfigurationName;
  private final GwtRunConfiguration myRunConfiguration;
  private @Nullable RemoteUiConnection myUiConnection;
  private int myRemoteUiPort;
  private Disposable myFrameListenerDisposable;
  private final GwtRunConfiguration.GwtRunConfigurationState myRunConfigurationState;
  private String myOutputDirPath;
  private ParametersList myPatchedShellParametersList;
  private final String myModuleDir;

  private File myAppEngineGeneratedDir;
  private Set<String> myOldGwtTempFiles;

  public GwtCommandLineState(@NotNull Module module, @NotNull GwtFacet facet, @NotNull ExecutionEnvironment environment,
                             @NotNull GwtRunConfiguration configuration, @NotNull GwtDevModeServer server,
                             @NotNull Executor executor) throws ExecutionException {
    super(environment);
    myFacet = facet;
    myModule = module; // may differ from facet.getModule()
    myFacetModule = facet.getModule();
    myProject = module.getProject();
    myRunConfiguration = configuration;
    myGwtModules = configuration.getGwtState().getGwtModules();
    myExecutor = executor;
    myRunConfigurationState = configuration.getGwtState();
    myRunConfigurationName = environment.getRunProfile().getName();
    myServer = server;
    myModuleDir = PathMacroUtil.getModuleDir(myModule.getModuleFilePath());
    if (isUseRemoteUi()) {
      myUiConnection = new RemoteUiConnection(configuration, facet.getSdkVersion(), myRunConfigurationState.OPEN_IN_BROWSER,
                                              myRunConfigurationState.START_JAVASCRIPT_DEBUGGER,
                                              WebBrowserManager.getInstance().findBrowserById(myRunConfigurationState.BROWSER));
      try {
        myRemoteUiPort = myUiConnection.open();
      }
      catch (IOException e) {
        throw new ExecutionException(GwtBundle.message("error.message.cannot.create.socket.0", e.getMessage()), e);
      }
    }
    initOutputDir();
  }

  public Module getModule() {
    return myModule;
  }

  private boolean isUseRemoteUi() {
    return myFacet.getSdkVersion().isOutOfProcessHostedModeSupported() && !SHOW_SHELL_WINDOW_FOR_DEV_MODE;
  }

  @Override
  protected void onException() {
    if (myUiConnection != null) {
      myUiConnection.close();
    }
    if (myFrameListenerDisposable != null) {
      Disposer.dispose(myFrameListenerDisposable);
      myFrameListenerDisposable = null;
    }
    if (myAppEngineGeneratedDir != null && !myModule.isDisposed()) {
      File appEngineDir = new File(getOutputDir() + FileUtil.toSystemDependentName("/" + APP_ENGINE_GENERATED_DIR));
      if (appEngineDir.isDirectory()) {
        try {
          FileUtil.copyDir(appEngineDir, myAppEngineGeneratedDir);
        }
        catch (IOException e) {
          LOG.info(e);
        }
      }
    }
    if (myOldGwtTempFiles != null) {
      File[] tempFiles = new File(FileUtil.getTempDirectory()).listFiles(GWT_TEMP_FILES_FILTER);
      if (tempFiles != null) {
        for (File file : tempFiles) {
          if (!myOldGwtTempFiles.contains(file.getName())) {
            FileUtil.delete(file);
          }
        }
      }
    }
  }

  @Override
  protected JavaParameters createJavaParametersImpl() throws ExecutionException {
    final JavaParameters params = new JavaParameters();

    if (getWorkingDirectoryPath() != null) {
      params.setWorkingDirectory(getWorkingDirectoryPath());
    }
    else {
      params.setWorkingDirectory(getOutputDir());
    }

    final PathsList classPath = params.getClassPath();
    for (String path : GwtClasspathUtil.getSourceRootsOfGwtModules(myModule, true).getPathList()) {
      classPath.add(path);
    }
    if (myFacetModule != myModule) {
      for (String path : GwtClasspathUtil.getSourceRootsOfGwtModules(myFacetModule, true).getPathList()) {
        classPath.add(path);
      }
    }

    final GwtVersion sdkVersion = myFacet.getSdkVersion();
    JavaParametersUtil.configureModule(myModule, params, JavaParameters.JDK_AND_CLASSES_AND_TESTS,
                                       myRunConfigurationState.ALTERNATIVE_JRE_PATH);

    if (SystemInfo.isMac) {
      if (!sdkVersion.isOutOfProcessHostedModeSupported()) {
        params.getVMParametersList().add("-XstartOnFirstThread");
      }
      // don't show Java Coffee Cup icon in the Dock on Mac OSX
      params.getVMParametersList().add("-Dapple.awt.UIElement=true");
    }
    addParametersString(params.getVMParametersList(), myRunConfigurationState.VM_PARAMETERS);

    final ParametersList programParameters = params.getProgramParametersList();
    if (sdkVersion.isDevModeSupportsOutputStyleOption()) {
      programParameters.add("-style");
      programParameters.add(myFacet.getConfiguration().getOutputStyle().getId());
    }
    // Consume the already-parsed parameters directly instead of re-joining and re-parsing them: an extra
    // join/parse round-trip corrupted lone dash-flag arguments such as -includeJsInteropExports (IDEA-339121).
    for (String param : myPatchedShellParametersList.getParameters()) {
      addParameter(programParameters, param);
    }
    if (myUiConnection != null) {
      programParameters.add("-remoteUI");
      programParameters.add(myRemoteUiPort + ":" + "IntelliJIdea");
    }
    String runPage = myRunConfigurationState.RUN_PAGE;
    if (sdkVersion.isModulesToLoadSpecifiedInDevMode()) {
      if (!StringUtil.isEmptyOrSpaces(runPage)) {
        programParameters.add("-startupUrl");
        programParameters.add(runPage);
      }
      addGwtModules(programParameters);
    }
    else {
      programParameters.add(runPage);
    }
    params.setupEnvs(myRunConfigurationState.ENVIRONMENT_VARIABLES, myRunConfigurationState.PASS_PARENT_ENVS);

    final GwtSdk sdk = myFacet.getConfiguration().getSdk();
    classPath.addFirst(sdk.getDevJarPath());
    IdeGwtDependenciesResolver dependenciesResolver = new IdeGwtDependenciesResolver(myProject);
    classPath.addAll(sdk.getGwtUserDependencies(dependenciesResolver));

    if (!myFacetModule.equals(myModule)) {
      // we do not process dependencies recursively for maven projects
      // (see MavenOrderEnumeratorHandler#shouldProcessDependenciesRecursively()}),
      // so classpath doesn't contain "myFacetModule" dependencies in such case
      OrderEnumerator.orderEntries(myFacetModule).runtimeOnly().recursively().classes().collectPaths(classPath);
    }

    classPath.addAll(sdk.getGwtDevDependencies(dependenciesResolver));

    if (myRunConfigurationState.USE_SUPER_DEV_MODE) {
      File sdmLauncher = getSuperDevModeLauncherLegacyJar();
      if (sdkVersion.isLegacyJarForNewSuperDevModeRequired() && !sdmLauncher.exists()) {
        LOG.error("Cannot start SuperDevMode: " + sdmLauncher.getAbsolutePath() + " doesn't exist");
      }
      else {
        classPath.addFirst(sdk.getCodeServerJarPath());
        if (sdkVersion.isLegacyJarForNewSuperDevModeRequired()) {
          classPath.addFirst(sdmLauncher.getAbsolutePath());
        }
        programParameters.addAt(0, "-superDevMode");
      }
    }
    else if (sdkVersion.isSuperDevModeUsedByDefault()) {
      programParameters.addAt(0, "-nosuperDevMode");
    }
    params.setMainClass(sdkVersion.getDevModeClass());

    // Always shorten a potentially long classpath via a classpath file rather than passing it literally: otherwise a
    // classpath exceeding the OS command-line limit (~33k chars) is silently truncated and tail dependencies such as
    // cern.colt.map.OpenIntObjectHashMap end up missing at runtime (IDEA-380767). Never use the 'manifest JAR' method:
    // the GWT dev/compiler manually searches for resources in URLs returned by URLClassLoader#getURLs, which does not
    // work with a manifest JAR. This mirrors the JPS compiler side (GwtBuilder#buildJavaCommandLine, useClasspathJar=false).
    params.setClasspathFile(true);
    params.setUseDynamicClasspath(true);

    myServer.patchParameters(params, getOutputDir().getAbsolutePath(), myFacet);
    return params;
  }

  private void addGwtModules(ParametersList programParameters) {
    if (myGwtModules != null) {
      programParameters.addAll(myGwtModules);
    }
    else {
      for (GwtModule module : GwtModulesManager.getInstance(myProject).getCompilableGwtModules(myFacetModule, false)) {
        programParameters.add(module.getQualifiedName());
      }
    }
  }

  private Collection<GwtModule> getGwtModules() {
    GwtModulesManager modulesManager = GwtModulesManager.getInstance(myProject);
    if (myGwtModules != null) {
      List<GwtModule> modules = new ArrayList<>();
      for (String module : myGwtModules) {
        ContainerUtil.addIfNotNull(modules, modulesManager.findGwtModuleByQualifiedName(module, myFacetModule.getModuleScope(false)));
      }
      return modules;
    }
    else {
      return modulesManager.getCompilableGwtModules(myFacetModule, false);
    }
  }

  private static @NotNull File getSuperDevModeLauncherLegacyJar() {
    File pluginClassesRoot = new File(PathUtil.getJarPathForClass(GwtCommandLineState.class));
    if (pluginClassesRoot.isFile()) {
      return new File(pluginClassesRoot.getParentFile(), "sdm-launcher/superdevmode-launcher-legacy.jar");
    }
    else {
      File pluginHome = PluginPathManager.getPluginHome("GwtStudio");
      return new File(pluginHome, "lib/superdevmode-launcher-legacy.jar");
    }
  }

  private File getOutputDir() {
    return new File(myOutputDirPath);
  }

  private File getTempOutputDir() {
    return new File(GwtCompilerPaths.getOutputRoot(myModule), "run");
  }

  private void initOutputDir() {
    ParametersList programParameters = new ParametersList();
    programParameters.addParametersString(myRunConfigurationState.SHELL_PARAMETERS);
    String outputDirParameterName = myFacet.getSdkVersion().getCompilerOutputDirParameterName();
    String outputDirPath = null;
    boolean hasOutputDirParameter = programParameters.hasParameter(outputDirParameterName);
    if (hasOutputDirParameter) {
      List<String> parametersList = programParameters.getList();
      int index = parametersList.indexOf(outputDirParameterName);
      outputDirPath = (index + 1 == parametersList.size()) ? null : parametersList.get(index + 1);
      if (outputDirPath == null || outputDirPath.startsWith("-")) {
        outputDirPath = null;
        hasOutputDirParameter = false;
      }
    }
    if (!hasOutputDirParameter) {
      programParameters.add(outputDirParameterName);
      outputDirPath = new File(getTempOutputDir(), "www").getAbsolutePath();
      String patchedOutputDir = myServer.patchWarDirectoryPath(outputDirPath);
      FileUtil.createDirectory(new File(patchedOutputDir));
      programParameters.add(patchedOutputDir);
    }
    String sourceLevel = myFacet.getConfiguration().getClientLanguageLevelString();
    if (sourceLevel != null) {
      programParameters.add("-sourceLevel", sourceLevel);
    }
    myPatchedShellParametersList = programParameters;
    myOutputDirPath = outputDirPath;
  }

  private String getWorkingDirectoryPath() {
    final String workingDirectory = StringUtil.nullize(myRunConfigurationState.WORKING_DIRECTORY);
    return workingDirectory == null ? null : expandModuleDir(workingDirectory);
  }

  @Override
  public @NotNull ExecutionResult executeImpl(final @NotNull Executor executor, final @NotNull ProgramRunner runner) throws ExecutionException {
    //noinspection ResultOfMethodCallIgnored
    getOutputDir().mkdirs();
    if (getWorkingDirectoryPath() != null) {
      //noinspection ResultOfMethodCallIgnored
      new File(getWorkingDirectoryPath()).mkdirs();
    }

    if (myRunConfigurationState.CUSTOM_WEB_XML != null) {
      File targetWebXml =
        new File(getOutputDir().getAbsolutePath() + "/tomcat/webapps/ROOT/WEB-INF/web.xml".replace('/', File.separatorChar));
      try {
        FileUtilRt.createParentDirs(targetWebXml);
        FileUtil.copy(new File(FileUtil.toSystemDependentName(VfsUtilCore.urlToPath(myRunConfigurationState.CUSTOM_WEB_XML))),
                      targetWebXml);
        patchWebXml(targetWebXml);
      }
      catch (IOException | JDOMException e) {
        LOG.info(e);
      }
    }

    HostedModeWarDirectoryGenerator generator = null;
    final WebFacet webFacet = myFacet.getWebFacet();
    if (myFacet.getSdkVersion().isHostedModeRequiresWebXml() && !hasNoServerParameter()) {
      generator = createDevModeWarDirectoryGenerator(getOutputDir(), new File(getTempOutputDir(), "files.dat"));
      if (myRunConfigurationState.USE_SUPER_DEV_MODE) {
        Collection<GwtModule> modules = getGwtModules();
        GlobalSearchScope scope =
          myFacetModule.getModuleWithDependenciesScope().intersectWith(GlobalSearchScopesCore.projectProductionScope(myProject));
        Collection<GwtModule> inheritedModules = GwtModulesManager.getInstance(myProject).findAllInheritedModules(modules, scope);
        Set<GwtModule> allModules = new HashSet<>(modules.size() + inheritedModules.size());
        allModules.addAll(modules);
        allModules.addAll(inheritedModules);

        for (GwtModule module : allModules) {
          for (VirtualFile file : module.getPublicRoots(false)) {
            generator.addFile(file, module.getOutputName());
          }
        }
      }
      if (webFacet != null) {
        //todo use extension point instead?
        final Collection<Artifact>
          artifacts = JavaeeArtifactUtil.getInstance()
          .getArtifactsContainingFacet(webFacet, WebArtifactUtil.getInstance().getExplodedWarArtifactType());
        final Artifact artifact = ContainerUtil.getFirstItem(artifacts, null);
        if (artifact != null) {
          final String explodedPath = artifact.getOutputPath();
          if (explodedPath != null && !explodedPath.isEmpty()) {
            myAppEngineGeneratedDir = new File(FileUtil.toSystemDependentName(explodedPath + "/" + APP_ENGINE_GENERATED_DIR));
            final VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(myAppEngineGeneratedDir);
            if (file != null) {
              WriteAction.run(() -> file.refresh(false, true));
              generator.addFile(file, APP_ENGINE_GENERATED_DIR);
            }
          }
        }
      }
      generator.generate(myProject);
    }

    if (generator != null) {
      assert myFrameListenerDisposable == null;
      myFrameListenerDisposable = Disposer.newDisposable();
      ApplicationManager.getApplication().getMessageBus().connect(myFrameListenerDisposable)
        .subscribe(ApplicationActivationListener.TOPIC, new MyFrameStateListener(generator));
    }

    final ConsoleView mainConsole = getConsoleBuilder().filters(new GwtErrorFilter(myModule)).getConsole();
    ConsoleView view;
    if (myUiConnection != null) {
      view = new RemoteUiView(myProject, mainConsole, myUiConnection, myExecutor.getId().equals(DefaultDebugExecutor.EXECUTOR_ID));
    }
    else {
      view = mainConsole;
    }
    myOldGwtTempFiles = Set.of(
      ObjectUtils.notNull(new File(FileUtil.getTempDirectory()).list(GWT_TEMP_FILES_FILTER), ArrayUtilRt.EMPTY_STRING_ARRAY));
    if (Boolean.parseBoolean(System.getProperty("idea.gwt.clear.unit.cache.before.run", "true"))) {
      File unitCacheDir = new File(getTempOutputDir(), "gwt-unitCache");
      LOG.debug("Clearing " + unitCacheDir.getAbsolutePath());
      FileUtil.delete(unitCacheDir);
    }
    final ProcessHandler processHandler = startProcess();
    mainConsole.attachToProcess(processHandler);

    processHandler.putUserData(GWT_GENERATOR_KEY, generator);
    processHandler.putUserData(GWT_CONFIGURATION_STATE_KEY, this);
    processHandler.addProcessListener(new GwtDevModeProcessListener());
    processHandler.addProcessListener(new GwtDevModeTerminationProcessListener());
    return new DefaultExecutionResult(view, processHandler, createActions(mainConsole, processHandler, executor));
  }

  private boolean hasNoServerParameter() {
    return myPatchedShellParametersList.hasParameter("-noserver")
           || myPatchedShellParametersList.hasParameter("-nostartServer");
  }

  private HostedModeWarDirectoryGenerator createDevModeWarDirectoryGenerator(final File warDirectory, final File cacheFile) {
    HostedModeWarDirectoryGenerator generator = new HostedModeWarDirectoryGenerator(warDirectory, cacheFile);
    final List<WebFacet> webFacets = new ArrayList<>();
    List<OrderEnumerator> enumerators = new ArrayList<>(2);
    enumerators.add(OrderEnumerator.orderEntries(myFacetModule).withoutSdk().productionOnly().runtimeOnly().recursively());
    if (!myFacetModule.equals(myModule)) {
      enumerators.add(OrderEnumerator.orderEntries(myModule).withoutSdk().productionOnly().runtimeOnly().recursively());
    }
    for (OrderEnumerator enumerator : enumerators) {
      enumerator.forEachModule(module -> {
        GwtFacet facet = GwtFacet.getInstance(module);
        if (facet != null) {
          ContainerUtil.addIfNotNull(webFacets, facet.getWebFacet());
        }
        else {
          webFacets.addAll(WebFacet.getInstances(module));
        }
        return true;
      });
    }
    for (WebFacet webFacet : webFacets) {
      for (WebRoot webRoot : webFacet.getWebRoots()) {
        final VirtualFile dir = webRoot.getFile();
        if (dir != null) {
          generator.addFile(dir, webRoot.getRelativePath());
        }
      }
    }

    final VirtualFile gwtUserJar = myFacet.getConfiguration().getSdk().getUserJar();
    boolean gwtServletJarFound = false;
    final String servletJarPath = FileUtil.toSystemIndependentName(myFacet.getConfiguration().getSdk().getServletJarPath());
    final VirtualFile gwtServletJar = LocalFileSystem.getInstance().findFileByPath(servletJarPath);
    for (OrderEnumerator enumerator : enumerators) {
      PathsList classpath = enumerator.getPathsList();
      for (VirtualFile file : classpath.getVirtualFiles()) {
        if (file.isDirectory()) {
          generator.addFile(file, "WEB-INF/classes");
        }
        else {
          if (gwtUserJar != null && gwtUserJar.equals(file)) {
            continue;
          }

          generator.addFile(file, "WEB-INF/lib/" + file.getName());
          if (file.equals(gwtServletJar)) {
            gwtServletJarFound = true;
          }
        }
      }
    }
    if (!gwtServletJarFound && gwtServletJar != null) {
      generator.addFile(gwtServletJar, "WEB-INF/lib/" + gwtServletJar.getName());
    }
    return generator;
  }

  public @Nullable RemoteUiConnection getUiConnection() {
    return myUiConnection;
  }

  public String getRunConfigurationName() {
    return myRunConfigurationName;
  }

  @Override
  protected AnAction @NotNull [] createActions(ConsoleView console, ProcessHandler processHandler, Executor executor) {
    final AnAction[] actions = super.createActions(console, processHandler, executor);
    if (myFacet.getSdkVersion().isHostedModeRequiresWebXml()) {
      final AnAction action = ActionManager.getInstance().getAction(IdeActions.ACTION_UPDATE_RUNNING_APPLICATION);
      if (action != null) {
        return ArrayUtil.append(actions, action);
      }
    }
    return actions;
  }

  private void patchWebXml(final File webXml) throws IOException, JDOMException {
    File devJar = new File(myFacet.getConfiguration().getSdk().getDevJarPath());
    if (!devJar.exists()) {
      return;
    }
    try (ZipFile zipFile = new ZipFile(devJar)) {
      ZipEntry zipEntry = zipFile.getEntry(WEB_XML_PATH);
      InputStream input = zipFile.getInputStream(zipEntry);
      Element rootElement = JDOMUtil.load(input);

      Element target = JDOMUtil.load(webXml);
      final Element targetRoot = target;
      ReadAction.runBlocking(() -> deleteGwtServlets(targetRoot));

      for (Element child : rootElement.getChildren()) {
        Element copy = child.clone();
        copy.setNamespace(targetRoot.getNamespace());
        for (Element grandChild : copy.getChildren()) {
          grandChild.setNamespace(targetRoot.getNamespace());
        }
        targetRoot.addContent(copy);
      }
      JDOMUtil.write(target, webXml.toPath());
    }
  }

  private void deleteGwtServlets(final Element root) {
    JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(myProject);
    GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(myModule);

    List<Element> toRemove = new ArrayList<>();
    Set<String> servletNamesToRemove = new HashSet<>();

    for (Element servlet : root.getChildren("servlet", root.getNamespace())) {
      String className = servlet.getChildTextTrim("servlet-class", root.getNamespace());
      if (className != null) {
        PsiClass psiClass = psiFacade.findClass(className, scope);
        if (RemoteServiceUtil.isRemoteServiceImplementation(psiClass)) {
          toRemove.add(servlet);
          servletNamesToRemove.add(servlet.getChildTextTrim("servlet-name", root.getNamespace()));
        }
      }
    }

    for (Element mapping : root.getChildren("servlet-mapping", root.getNamespace())) {
      String servletName = mapping.getChildTextTrim("servlet-name", root.getNamespace());
      if (servletNamesToRemove.contains(servletName)) {
        toRemove.add(mapping);
      }
    }

    for (Element element : toRemove) {
      root.removeContent(element);
    }
  }

  private void addParametersString(ParametersList list, String params) {
    for (String param : ParametersList.parse(params)) {
      addParameter(list, param);
    }
  }

  private void addParameter(ParametersList list, String param) {
    list.add(expandModuleDir(param));
  }

  private String expandModuleDir(String value) {
    // Expand project-level path macros ($PROJECT_DIR$, $USER_HOME$, ...) in addition to $MODULE_DIR$ so that macros
    // used in run-configuration parameters and the working directory are resolved at execution time (IDEA-377758).
    final String expanded = PathMacroManager.getInstance(myProject).expandPath(value);
    if (myModuleDir != null && expanded.contains(PathMacroUtil.DEPRECATED_MODULE_DIR)) {
      return expanded.replace(PathMacroUtil.DEPRECATED_MODULE_DIR, myModuleDir);
    }
    return expanded;
  }

  private static final class IdeGwtDependenciesResolver implements GwtDependenciesResolver {
    private final Project myProject;
    private final GwtDependenciesStorage myDependenciesStorage;

    private IdeGwtDependenciesResolver(Project project) {
      myProject = project;
      myDependenciesStorage = GwtDependenciesStorage.getInstance(project);
    }

    @Override
    public @NotNull List<String> resolveDependencies(@NotNull String groupId, @NotNull String artifactId, @NotNull String version) {
      try {
        return myDependenciesStorage.getDependenciesPaths(groupId, artifactId, version);
      }
      catch (JpsGwtDependenciesStorage.GwtDependenciesNotFoundException e) {
        new Notification(NotificationGroup.createIdWithTitle("GWT", GwtBundle.message("gwt.notification.display.id")), e.getMessage(),
                         NotificationType.WARNING)
          .setImportant(false)
          .notify(myProject);

        scheduleArtifactPathsResolve(List.of(new MavenCoordinates(groupId, artifactId, version)));

        return emptyList();
      }
    }

    @Override
    public @NotNull List<String> resolveAnyOf(@NotNull Collection<MavenCoordinates> alternatives) {
      List<String> messages = new ArrayList<>();

      for (MavenCoordinates alternative : alternatives) {
        try {
          return myDependenciesStorage.getDependenciesPaths(alternative.groupId, alternative.artifactId, alternative.version);
        }
        catch (JpsGwtDependenciesStorage.GwtDependenciesNotFoundException e) {
          messages.add(e.getMessage());
        }
      }

      new Notification(NotificationGroup.createIdWithTitle("GWT", GwtBundle.message("gwt.notification.display.id")), StringUtil.join(messages, "\n"),
                       NotificationType.WARNING)
        .setImportant(false)
        .notify(myProject);

      scheduleArtifactPathsResolve(alternatives);

      return emptyList();
    }

    private void scheduleArtifactPathsResolve(Collection<MavenCoordinates> coordinates) {
      // invokeAndWait is not allowed under read action, so we can only refresh asynchronously
      ApplicationManager.getApplication().invokeLater(() -> {
        for (MavenCoordinates coordinate : coordinates) {
          JpsMavenRepositoryLibraryDescriptor descriptor =
            new JpsMavenRepositoryLibraryDescriptor(coordinate.groupId, coordinate.artifactId, coordinate.version);
          Collection<OrderRoot> roots = JarRepositoryManager.loadDependenciesModal(
            myProject,
            descriptor,
            Collections.singleton(ArtifactKind.ARTIFACT),
            null,
            null
          );
          List<String> resolvedPaths = ContainerUtil.map(roots, root -> VfsUtilCore.virtualToIoFile(root.getFile()).getAbsolutePath());
          myDependenciesStorage.storeDependenciesPaths(coordinate.groupId, coordinate.artifactId, coordinate.version, resolvedPaths);
        }
      }, ModalityState.nonModal(), myProject.getDisposed());
    }
  }

  private class GwtDevModeTerminationProcessListener extends ProcessAdapter {

    @Override
    public void processTerminated(@NotNull ProcessEvent event) {
      onException();
    }
  }

  private static class GwtDevModeProcessListener extends ProcessAdapter {

    private boolean myOpenUrl;
    private final OutputLineReader myLineReader = new DelegatingOutputLineReader(this::parseLine);

    @Override
    public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
      if (ProcessOutputType.isStderr(outputType)) {
        myLineReader.parseOutput(event.getText());
      }
    }

    private void parseLine(@NotNull @NonNls String line) {
      if (line.endsWith("please browse to")) {
        myOpenUrl = true;
      }
      else if (!line.equals("the following URL:")) {
        if (myOpenUrl) {
          BrowserUtil.browse(line);
        }
        myOpenUrl = false;
      }
    }
  }

  private class MyFrameStateListener implements ApplicationActivationListener {
    private final HostedModeWarDirectoryGenerator myGenerator;

    MyFrameStateListener(@NotNull HostedModeWarDirectoryGenerator generator) {
      myGenerator = generator;
    }

    @Override
    public void applicationDeactivated(@NotNull IdeFrame ideFrame) {
      if (myRunConfiguration.getGwtState().UPDATE_RESOURCES_ON_FRAME_DEACTIVATION) {
        myGenerator.updateResources(myProject, myRunConfigurationName);
      }
    }
  }
}
