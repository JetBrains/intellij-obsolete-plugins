// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.config;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.JavadocOrderRootType;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NlsContexts.NotificationContent;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotifications;
import com.intellij.util.PathUtil;
import com.intellij.util.SystemProperties;
import com.intellij.util.concurrency.ThreadingAssertions;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.runner.GrailsCommandExecutorUtil;
import org.jetbrains.plugins.grails.runner.GrailsConsole;
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager;
import org.jetbrains.plugins.grails.structure.OldGrailsApplication;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.mvc.MvcCommand;
import org.jetbrains.plugins.groovy.mvc.MvcModuleStructureUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"Convert2Diamond", "Convert2Lambda"})
public class GrailsSettingSynchronizer {
  private static final Logger LOG = Logger.getInstance(GrailsSettingSynchronizer.class);

  private static final String PRINT_SCRIPT_NAME = "IdeaPrintProjectSettings.groovy";
  private static final String SCRIPT_DIRECTORY = ".grails/scripts";
  private static final Pattern DEPENDENCY_PATTERN = Pattern.compile("([A-Za-z]+)\\.\\d+");

  private static final Key<Boolean> UPDATING_SETTINGS = Key.create("grails.updating.settings");

  // /home/sergey/.ivy2/cache/net.sf.ehcache/ehcache-web/javadocs/ehcache-web-2.0.0-javadoc.jar!/
  // /home/sergey/.ivy2/cache/net.sf.ehcache/ehcache-web/jars/ehcache-web-2.0.0.jar!/
  // /home/sergey/.ivy2/cache/net.sf.ehcache/ehcache-web/sources/ehcache-web-2.0.0-sources.jar!/
  private static final Pattern ARTIFACT_TYPE_PATTERN = Pattern.compile(".+/([^/]+)/(?:sources|javadocs)/\\1-[^/]+-(javadoc|sources)\\.jar\\!/");
  private static final Pattern IVY_JAR_PATTERN = Pattern.compile("(.+)/([^/]+)/jars/\\2-([^/]+)\\.jar\\!/");
  private static final Pattern MAVEN_JAR_PATTERN = Pattern.compile("(.+)/([^/]+)/([^/]+)/\\2-\\3\\.jar\\!/");
  private static final Pattern DIST_PATTERN = Pattern.compile("(.+)/dist/([^/]+)-([^/]+)\\.jar\\!/");

  protected final Module myModule;

  public GrailsSettingSynchronizer(Module module) {
    this.myModule = module;
  }

  public static boolean isUpdateSettingRunning(@NotNull Project project) {
    if (project.getUserData(GrailsFramework.UPDATE_IN_PROGRESS) == Boolean.TRUE) return true;
    for (Module m : ModuleManager.getInstance(project).getModules()) {
      if (m.getUserData(UPDATING_SETTINGS) == Boolean.TRUE) return true;
    }
    return false;
  }

  protected void onDone(boolean isSettingChanged, boolean isExtractingSettingsRan) {
    if (isSettingChanged) {
      final VirtualFile buildConfigFile = getBuildConfigFile();
      if (buildConfigFile != null) {
        EditorNotifications.getInstance(myModule.getProject()).updateNotifications(buildConfigFile);
      }
    }
  }

  private @Nullable String getBuildConfigText() {
    final VirtualFile buildConfigFile = getBuildConfigFile();
    if (buildConfigFile != null) {
      try {
        return VfsUtilCore.loadText(buildConfigFile);
      }
      catch (IOException e) {
        LOG.error(e);
      }
    }
    return null;
  }

  private @Nullable VirtualFile getBuildConfigFile() {
    VirtualFile appDirectory = GrailsFramework.getInstance().findAppDirectory(myModule);
    if (appDirectory != null) {
      return VfsUtil.findRelativeFile(appDirectory, GrailsUtils.CONF_DIRECTORY, GrailsUtils.BUILD_CONFIG);
    }
    else {
      return null;
    }
  }

  private void showMessage(final @NotificationContent String message, final MessageType type) {
    if (ApplicationManager.getApplication().isUnitTestMode()) return;

    GrailsConsole.getInstance(myModule.getProject());

    GrailsConsole.NOTIFICATION_GROUP.createNotification(message, type).notify(myModule.getProject());
  }

  private static String readJvmOptions(@NotNull Module module) {
    VirtualFile root = GrailsUtils.findGrailsAppRoot(module);
    if (root == null) return null;

    VirtualFile vmOptionFile = root.findChild("grails-synch-jvm-options.txt");
    if (vmOptionFile == null) return null;

    return LoadTextUtil.loadText(vmOptionFile).toString().replaceAll("\\s*\\n\\s*", " ").trim();
  }

  public boolean syncGrailsSettings(final boolean isCalledByUser) {
    ThreadingAssertions.assertEventDispatchThread();

    FileDocumentManager.getInstance().saveAllDocuments();

    try {
      final GrailsSettings settings = GrailsSettingsService.getGrailsSettings(myModule);

      String buildConfigText = getBuildConfigText();

      int pluginDependenciesCrc = GrailsUtils.getPluginDependenciesCrc(myModule);

      if (buildConfigText == null && pluginDependenciesCrc == 0) {
        boolean isChanged;

        if (settings.buildConfigCrc != null && settings.buildConfigCrc == 0 &&
            settings.pluginDependenciesCrc == 0 &&
            settings.properties.isEmpty() &&
            settings.customPluginLocations.isEmpty()) {
          isChanged = false;
        }
        else {
          WriteAction.run(() -> {
            settings.buildConfigCrc = 0;
            settings.pluginDependenciesCrc = 0;
            settings.customPluginLocations.clear();
            settings.properties.clear();
          });

          isChanged = true;
        }

        if (isChanged || isCalledByUser) {
          showMessage(GrailsBundle.message("notification.content.settings.sync.completed"), MessageType.INFO);
        }
        onDone(isChanged, false);
        return true;
      }

      if (!isCalledByUser && !settings.isBuildConfigOutdated(buildConfigText) && settings.pluginDependenciesCrc == pluginDependenciesCrc) {
        onDone(false, false);
        return true;
      }

      if (ApplicationManager.getApplication().isUnitTestMode()) return true;

      // Reload Settings from BuildConfig.groovy

      if (!ensureScriptExists(PRINT_SCRIPT_NAME, SCRIPT_DIRECTORY)) return false;

      GeneralCommandLine commandLine;

      try {
        OldGrailsApplication application = GrailsApplicationManager.findApplication(myModule);
        if (application == null) {
          throw new ExecutionException(
            GrailsBundle.message("dialog.message.cannot.find.grails.application.for.module", myModule.getName())
          );
        }
        commandLine = GrailsCommandExecutorUtil.createCommandLine(
          application,
          MvcCommand.parse("idea-print-project-settings").setVmOptions(readJvmOptions(myModule))
        );
      }
      catch (ExecutionException e) {
        if (isCalledByUser) {
          Notifications.Bus.notify(
            new Notification(GrailsUtils.GRAILS_NOTIFICATION_GROUP, GrailsBundle.message("notification.title.failed.to.run.grails.command"), e.getMessage(), NotificationType.ERROR)
          );
        }
        return false;
      }

      final StringBuilder output = new StringBuilder();

      final ProcessListener listener = new ProcessListener() {
        @Override
        public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
          if (outputType == ProcessOutputTypes.STDOUT) {
            output.append(StringUtil.convertLineSeparators(event.getText()));
          }
        }
      };

      myModule.putUserData(UPDATING_SETTINGS, true);

      GrailsConsole.getInstance(myModule.getProject()).executeProcess(commandLine, new Runnable() {
        @Override
        public void run() {
          myModule.getProject().putUserData(GrailsFramework.UPDATE_IN_PROGRESS, true);
          myModule.putUserData(UPDATING_SETTINGS, false);

          Ref<Boolean> isChanged = new Ref<Boolean>();
          if (processOutput(myModule, output.toString(), isChanged)) {
            if (isCalledByUser || isChanged.get()) {
              showMessage(GrailsBundle.message("notification.content.settings.sync.completed"), MessageType.INFO);
            }
            onDone(isChanged.get(), true);
          }
          else {
            showMessage(GrailsBundle.message("notification.content.settings.sync.failed"), MessageType.ERROR);
            GrailsConsole.getInstance(myModule.getProject()).show(null, true);
          }

          myModule.getProject().putUserData(GrailsFramework.UPDATE_IN_PROGRESS, null);
          DaemonCodeAnalyzer.getInstance(myModule.getProject()).restart(this);
        }
      }, isCalledByUser, true).addProcessListener(listener);

      return false;
    }
    finally {
      DaemonCodeAnalyzer.getInstance(myModule.getProject()).restart(this);
    }
  }

  public static boolean ensureScriptExists(@NotNull String name, @NotNull String directory) {
    byte[] bundledScriptContents;
    try (InputStream inputStream = GrailsSettingSynchronizer.class.getResourceAsStream('/' + name)) {
      if (inputStream == null) throw new IllegalArgumentException(name);
      bundledScriptContents = inputStream.readAllBytes();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }

    Path script = Paths.get(SystemProperties.getUserHome(), directory, name);

    if (!contentEquals(bundledScriptContents, script)) {
      try {
        Files.createDirectories(Paths.get(SystemProperties.getUserHome(), directory));
        try {
          Files.createFile(script);
        } catch (FileAlreadyExistsException ignored) {
        }
        Files.write(script, bundledScriptContents);
      }
      catch (Exception e) {
        Notifications.Bus.notify(new Notification(
          GrailsUtils.GRAILS_NOTIFICATION_GROUP,
          GrailsBundle.message("notification.title.grails.setting.synchronization.failed"),
          GrailsBundle.message("notification.content.failed.to.create.0.see.log.for.more.details", script), NotificationType.ERROR
        ));
        LOG.warn("Failed to create " + script, e);
        return false;
      }
    }
    return true;
  }

  private static boolean contentEquals(byte[] expected, Path file) {
    try {
      byte[] actual = Files.readAllBytes(file);
      return Arrays.equals(expected, actual);
    }
    catch (IOException e) {
      return false;
    }
  }

  private static List<VirtualFile> getPomFiles(@NotNull Module module) {
    List<VirtualFile> result = new ArrayList<VirtualFile>();

    Set<Module> dependentModules = new HashSet<Module>();
    ModuleUtilCore.getDependencies(module, dependentModules);

    for (Module m : dependentModules) {
      for (VirtualFile root : ModuleRootManager.getInstance(m).getContentRoots()) {
        ContainerUtil.addIfNotNull(result, root.findChild("pom.xml"));
      }
    }

    return result;
  }

  private static Set<String> getNonManagedJarNames(Module module) {
    final Set<String> coreJars = new HashSet<String>();

    for (VirtualFile file : OrderEnumerator.orderEntries(module).getAllLibrariesAndSdkClassesRoots()) {
      coreJars.add(VfsUtil.getLocalFile(file).getName());
    }

    Library defaultLibrary = MvcModuleStructureUtil.findUserLibrary(module, GrailsUtils.GRAILS_USER_LIBRARY);
    if (defaultLibrary != null) {
      for (VirtualFile file : defaultLibrary.getFiles(OrderRootType.CLASSES)) {
        coreJars.remove(VfsUtil.getLocalFile(file).getName());
      }
    }
    return coreJars;
  }

  private boolean processOutput(final Module module, final String output, Ref<Boolean> isChanged) {
    if (!GrailsFramework.getInstance().hasSupport(myModule)) return false;

    MultiMap<String, VirtualFile> deps = new MultiMap<String, VirtualFile>();

    final Map<String, String> properties = new HashMap<String, String>();
    final Map<String, String> customPluginLocations = new HashMap<String, String>();

    if (!parseOutput(deps, properties, customPluginLocations, output)) {
      return false;
    }

    List<VirtualFile> dependenciesToPoms = getPomFiles(myModule);
    Map<VirtualFile, byte[]> contentCache = new HashMap<VirtualFile, byte[]>();

    Set<String> nonManagedJarNames = getNonManagedJarNames(myModule);

    for (Iterator<? extends VirtualFile> itr = deps.values().iterator(); itr.hasNext(); ) {
      VirtualFile virtualFile = itr.next();
      if (nonManagedJarNames.contains(virtualFile.getName())) {
        itr.remove();
      }
      else {
        VirtualFile jar = JarFileSystem.getInstance().getVirtualFileForJar(virtualFile);
        if (jar == null) continue;
        String path = jar.getPath();
        if (!path.endsWith(".jar")) continue;

        String pomPath = path.substring(0, path.length() - ".jar".length()) + ".pom";  // check for maven repository.

        VirtualFile pom = LocalFileSystem.getInstance().refreshAndFindFileByPath(pomPath);
        if (pom == null) {
          // check for ~/.grails/ivy-cache/ ... /*.jar
          VirtualFile jarDir = jar.getParent();
          if (jarDir != null && "jars".equals(jarDir.getName())) {
            VirtualFile artifactDir = jarDir.getParent();
            if (artifactDir != null) {
              String artifactName = artifactDir.getName();
              String jarName = jar.getName();
              if (jarName.startsWith(artifactName) && jarName.length() > artifactName.length() + ".jar".length()) {
                String version = jarName.substring(artifactName.length(), jarName.length() - ".jar".length());
                pom = artifactDir.findChild("ivy" + version + ".xml.original");
              }
            }
          }

          if (pom == null) continue;
        }

        for (VirtualFile dependPom : dependenciesToPoms) {
          if (equalsByContent(pom, dependPom, contentCache)) {
            itr.remove(); // Don't add dependency to jar, because jar was build by module which already exists in dependencies.
            break;
          }
        }
      }
    }

    final Set<VirtualFile> compileDeps = new LinkedHashSet<VirtualFile>(deps.get(PrintGrailsSettingsConstants.COMPILE));

    compileDeps.addAll(deps.get(PrintGrailsSettingsConstants.RUNTIME));
    compileDeps.addAll(deps.get(PrintGrailsSettingsConstants.TESTS));
    compileDeps.addAll(deps.get(PrintGrailsSettingsConstants.BUILD));
    compileDeps.addAll(deps.get(PrintGrailsSettingsConstants.PROVIDED));

    //it seems not necessary to create separate library for test-only jars, Grails understands 'test' as runtime also
    //runtimeDeps.removeAll(compileDeps);
    //testDeps.removeAll(compileDeps);

    final GrailsSettings settings = GrailsSettingsService.getGrailsSettings(module);

    final String buildConfigText = getBuildConfigText();

    isChanged.set(!properties.equals(settings.properties) || !customPluginLocations.equals(settings.customPluginLocations));

    WriteAction.run(() -> {
      GrailsFramework framework = GrailsFramework.getInstance(module);
      if (framework != null) {
        settings.properties = properties;
        settings.customPluginLocations = customPluginLocations;
        settings.updateBuildConfig(buildConfigText);
        settings.pluginDependenciesCrc = GrailsUtils.getPluginDependenciesCrc(module);

        applyDefaultLibraryChanges(module, compileDeps);
      }
    });

    return true;
  }

  private static boolean equalsByContent(VirtualFile file1, VirtualFile file2, Map<VirtualFile, byte[]> contentCache) {
    if (file1.getLength() != file2.getLength()) return false;
    try {
      return Arrays.equals(getContent(file1, contentCache), getContent(file2, contentCache));
    }
    catch (IOException e) {
      LOG.warn("Failed to read file", e);
      return false;
    }
  }

  private static byte[] getContent(VirtualFile file, Map<VirtualFile, byte[]> contentCache) throws IOException {
    byte[] res = contentCache.get(file);
    if (res == null) {
      res = file.contentsToByteArray();
      contentCache.put(file, res);
    }

    return res;
  }

  private boolean parseOutput(MultiMap<String, VirtualFile> deps,
                              Map<String, String> properties,
                              Map<String, String> customPlugins,
                              String output) {
    int startIndex = output.indexOf(PrintGrailsSettingsConstants.SETTINGS_START_MARKER);
    if (startIndex == -1) {
      return false;
    }

    int endIndex = output.indexOf(PrintGrailsSettingsConstants.SETTINGS_END_MARKER);
    if (endIndex < startIndex) {
      return false;
    }

    Properties props = new Properties();

    try {
      props.load(
        new ByteArrayInputStream(output.substring(startIndex + PrintGrailsSettingsConstants.SETTINGS_START_MARKER.length(), endIndex).getBytes(
          StandardCharsets.UTF_8)));
    }
    catch (IOException e) {
      return false;
    }

    if (props.isEmpty()) { // Error occurred.
      return false;
    }

    VirtualFile appRoot = null;

    for (Map.Entry<Object, Object> entry : props.entrySet()) {
      String key = (String)entry.getKey();
      String value = (String)entry.getValue();

      if (key.startsWith(PrintGrailsSettingsConstants.CUSTOM_PLUGIN_PREFIX)) {
        customPlugins.put(key.substring(PrintGrailsSettingsConstants.CUSTOM_PLUGIN_PREFIX.length()), value);
      }
      else {
        Matcher matcher = DEPENDENCY_PATTERN.matcher(key);
        if (matcher.matches()) {
          String systemIndependentPath = FileUtil.toSystemIndependentName(value);
          if (!new File(systemIndependentPath).isAbsolute()) {
            if (appRoot == null) {
              appRoot = GrailsFramework.getInstance().findAppRoot(myModule);
            }

            if (appRoot != null) {
              systemIndependentPath = appRoot.getPath() + '/' + systemIndependentPath;
            }
          }

          VirtualFile dependency = LocalFileSystem.getInstance().refreshAndFindFileByPath(systemIndependentPath);

          if (dependency != null) {
            VirtualFile jarRoot = JarFileSystem.getInstance().getJarRootForLocalFile(dependency);

            final VirtualFile toAdd = jarRoot != null ? jarRoot : dependency;

            deps.putValue(matcher.group(1), toAdd);
          }
        }
        else {
          properties.put(key, value);
        }
      }
    }

    return true;
  }

  private static OrderRootType getArtifactType(String url) {
    Matcher matcher = ARTIFACT_TYPE_PATTERN.matcher(url);
    if (!matcher.matches()) {
      return OrderRootType.CLASSES;
    }

    String type = matcher.group(2);
    if (type.equals("sources")) {
      return OrderRootType.SOURCES;
    }

    assert type.equals("javadoc");

    return JavadocOrderRootType.getInstance();
  }

  private static void applyDefaultLibraryChanges(Module module, Set<VirtualFile> compileDeps) {
    final ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();
    final Library.ModifiableModel modifiableModel = MvcModuleStructureUtil.modifyDefaultLibrary(model, GrailsUtils.GRAILS_USER_LIBRARY);

    Set<VirtualFile> exists = new HashSet<VirtualFile>();

    for (VirtualFile file : modifiableModel.getFiles(OrderRootType.CLASSES)) {
      if (!compileDeps.contains(file) || OrderRootType.CLASSES != getArtifactType(file.getUrl())) {
        modifiableModel.removeRoot(file.getUrl(), OrderRootType.CLASSES);
      }
      else {
        exists.add(file);
      }
    }

    for (VirtualFile file : modifiableModel.getFiles(OrderRootType.SOURCES)) {
      if (!compileDeps.contains(file) && OrderRootType.SOURCES == getArtifactType(file.getUrl())) {
        modifiableModel.removeRoot(file.getUrl(), OrderRootType.SOURCES);
      }
      else {
        exists.add(file);
      }
    }

    OrderRootType javadocType = JavadocOrderRootType.getInstance();

    for (VirtualFile file : modifiableModel.getFiles(javadocType)) {
      if (!compileDeps.contains(file) && javadocType == getArtifactType(file.getUrl())) {
        modifiableModel.removeRoot(file.getUrl(), javadocType);
      }
      else {
        exists.add(file);
      }
    }

    for (VirtualFile compileDep : compileDeps) {
      OrderRootType type = getArtifactType(compileDep.getUrl());

      if (!exists.contains(compileDep)) {
        modifiableModel.addRoot(compileDep, type);
      }

      if (type == OrderRootType.CLASSES) {
        String jarPath = compileDep.getPath();

        Matcher matcher = IVY_JAR_PATTERN.matcher(jarPath);
        if (matcher.matches()) {
          String path = matcher.group(1);
          String name = matcher.group(2);
          String version = matcher.group(3);

          VirtualFile sources = JarFileSystem.getInstance()
            .findFileByPath(path + "/" + name + "/sources/" + name + "-" + version + "-sources.jar!/");
          if (sources != null && !exists.contains(sources)) {
            modifiableModel.addRoot(sources, OrderRootType.SOURCES);
            exists.add(sources);
          }

          String home = PathUtil.getParentPath(PathUtil.getParentPath(path));
          if (!StringUtil.isEmpty(home)) {
            sources = JarFileSystem.getInstance().findFileByPath(home + "/src/" + name + '-' + version + "-sources.jar!/");
            if (sources != null && !exists.contains(sources)) {
              modifiableModel.addRoot(sources, OrderRootType.SOURCES);
              exists.add(sources);
            }
          }

          VirtualFile javadoc = JarFileSystem.getInstance()
            .findFileByPath(path + '/' + name + "/javadocs/" + name + '-' + version + "-javadoc.jar!/");
          if (javadoc != null && !exists.contains(javadoc)) {
            modifiableModel.addRoot(javadoc, javadocType);
            exists.add(sources);
          }
        }
        else {
          matcher = MAVEN_JAR_PATTERN.matcher(jarPath);
          if (matcher.matches()) {
            String path = matcher.group(1);
            String artifactId = matcher.group(2);
            String version = matcher.group(3);

            VirtualFile sources = JarFileSystem.getInstance()
              .findFileByPath(path + '/' + artifactId + '/' + version + '/' + artifactId + '-' + version + "-sources.jar!/");
            if (sources != null && !exists.contains(sources)) {
              modifiableModel.addRoot(sources, OrderRootType.SOURCES);
              exists.add(sources);
            }

            VirtualFile javadoc = JarFileSystem.getInstance()
              .findFileByPath(path + '/' + artifactId + "/" + version + '/' + artifactId + '-' + version + "-javadoc.jar!/");
            if (javadoc != null && !exists.contains(javadoc)) {
              modifiableModel.addRoot(javadoc, javadocType);
              exists.add(sources);
            }
          }
          else {
            matcher = DIST_PATTERN.matcher(jarPath);
            if (matcher.matches()) {
              final VirtualFile sources = JarFileSystem.getInstance().findFileByPath(jarPath.replace(".jar!/", "-sources.jar!/"));
              if (sources != null && exists.add(sources)) modifiableModel.addRoot(sources, OrderRootType.SOURCES);

              final VirtualFile javadoc = JarFileSystem.getInstance().findFileByPath(jarPath.replace(".jar!/", "-javadoc.jar!/"));
              if (javadoc != null && exists.contains(javadoc)) modifiableModel.addRoot(javadoc, javadocType);
            }
          }
        }
      }
    }

    if (modifiableModel.isChanged()) {
      modifiableModel.commit();
      model.commit();
    }
    else {
      Disposer.dispose(modifiableModel);
      model.dispose();
    }
  }
}
