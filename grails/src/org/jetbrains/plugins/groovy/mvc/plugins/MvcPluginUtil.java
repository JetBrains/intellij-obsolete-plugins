// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.mvc.plugins;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.net.HttpConfigurable;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.runner.GrailsCommandExecutor;
import org.jetbrains.plugins.grails.runner.GrailsCommandExecutorUtil;
import org.jetbrains.plugins.grails.runner.GrailsCommandLineExecutor;
import org.jetbrains.plugins.grails.runner.GrailsConsole;
import org.jetbrains.plugins.grails.structure.OldGrailsApplication;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.mvc.MvcCommand;
import org.jetbrains.plugins.groovy.mvc.MvcModuleStructureUtil;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class MvcPluginUtil {
  private static final Logger LOG = Logger.getInstance(MvcPluginUtil.class);
  private static final String SET_PROXY_COMMAND = "set-proxy";
  private static final String ENTER_HTTP_PROXY_HOST = "Enter HTTP proxy host";
  private static final String ENTER_HTTP_PROXY_PORT = "Enter HTTP proxy port";
  private static final String ENTER_HTTP_PROXY_USERNAME = "Enter HTTP proxy username";
  private static final String ENTER_HTTP_PROXY_PASSWORD = "Enter HTTP proxy password";
  public static final Supplier<Color> COLOR_REMOVE_PLUGIN = () -> FileStatus.DELETED_FROM_FS.getColor();
  public static final Supplier<Color> COLOR_INSTALL_PLUGIN = () -> FileStatus.ADDED.getColor();
  public static final @NonNls String LIST_PLUGINS_COMMAND = "list-plugins";

  private static final Key<Pair<Collection<MvcPluginDescriptor>, Long>> PLUGIN_DESCRIPTOR_KEY = Key.create("MVC_PLUGIN_DESCRIPTOR_KEY");

  public static final Key<Boolean> PLUGIN_LIST_DONOT_DOWNLOADED = Key.create("PLUGIN_LIST_DONOT_DOWNLOADED");

  private MvcPluginUtil() { }

  private static void setProxyOld(final @NotNull HttpConfigurable cfg, @NotNull OldGrailsApplication application) {
    try {
      GrailsCommandExecutor executor = GrailsCommandExecutor.getGrailsExecutor(application);
      if (!(executor instanceof GrailsCommandLineExecutor)) return;
      final GeneralCommandLine commandLine = ((GrailsCommandLineExecutor)executor).createCommandLine(
              application, new MvcCommand(SET_PROXY_COMMAND)
      );
      final OSProcessHandler handler = new OSProcessHandler(commandLine);

      GrailsConsole.getInstance(application.getProject()).getConsole().attachToProcess(handler);
      final OutputStreamWriter writer = new OutputStreamWriter(handler.getProcess().getOutputStream(), StandardCharsets.UTF_8);
      writer.write(cfg.USE_HTTP_PROXY ? "y\n" : "n\n");
      writer.flush();

      handler.addProcessListener(new ProcessListener() {

        @Override
        public void onTextAvailable(final @NotNull ProcessEvent event, final @NotNull Key outputType) {
          String text = event.getText();

          try {
            if (text.contains(ENTER_HTTP_PROXY_HOST)) {
              writer.append(cfg.PROXY_HOST).append('\n');
            }
            else if (text.contains(ENTER_HTTP_PROXY_PORT)) {
              writer.write(cfg.PROXY_PORT);
              writer.write('\n');
            }
            else if (text.contains(ENTER_HTTP_PROXY_USERNAME)) {
              writer.append(cfg.getProxyLogin()).append('\n');
            }
            else if (text.contains(ENTER_HTTP_PROXY_PASSWORD)) {
              writer.append(cfg.getPlainProxyPassword()).append('\n');
            }
            writer.flush();
          }
          catch (IOException e) {
            LOG.debug(e);
          }
        }
      });

      handler.startNotify();
    }
    catch (Exception e) {
      GrailsConsole.NOTIFICATION_GROUP
              .createNotification(GrailsBundle.message("mvc.plugins.notification.title.process.cannot.start.set.proxy.process"), e.getMessage(), NotificationType.ERROR)
              .notify(application.getProject());
      LOG.debug(e);
    }
  }

  private static void setProxyNew(@NotNull HttpConfigurable cfg, final @NotNull OldGrailsApplication application) {
    if (cfg.USE_HTTP_PROXY) {
      MvcCommand command = new MvcCommand("add-proxy", "IDEA_PROXY", "--host=" + cfg.PROXY_HOST,
              "--port=" + cfg.PROXY_PORT, "--username=" + cfg.getProxyLogin(),
              "--password=" + cfg.getPlainProxyPassword());
      GrailsCommandExecutorUtil.executeInModal(
              application, command, GrailsBundle.message("progress.text.execute.add.proxy.command"),
              () -> GrailsCommandExecutorUtil.executeInModal(
                      application,
                      new MvcCommand("set-proxytitle", "IDEA_PROXY"),
                      GrailsBundle.message("progress.text.execute.set.proxy.command"),
                      null,
                      false
              ), false
      );
    }
    else {
      GrailsCommandExecutorUtil.executeInModal(application,
              new MvcCommand("clear-proxy"),
              GrailsBundle.message("progress.text.execute.clear.proxy.command"),
              null,
              false);
    }
  }

  public static void setFrameworkProxy(@NotNull HttpConfigurable cfg, @NotNull OldGrailsApplication application) {
    assert !GrailsConsole.getInstance(application.getProject()).isExecuting();

    if (application.getGrailsVersion().isAtLeast("1.3.2")) {
      setProxyNew(cfg, application);
    }
    else {
      setProxyOld(cfg, application);
    }
  }

  public static String cleanPath(final String path) {
    String pluginFilePath = path;
    if (path.endsWith("!/")) {
      pluginFilePath = path.substring(0, path.length() - "!/".length());
    }
    else if (path.endsWith("!")) {
      pluginFilePath = path.substring(0, path.length() - "!".length());
    }
    return pluginFilePath;
  }

  public static @NotNull List<MvcPluginDescriptor> refreshAndLoadPluginList(@NotNull OldGrailsApplication application) {
    VirtualFile sdkWorkDir = MvcModuleStructureUtil.refreshAndFind(GrailsFramework.getInstance().getSdkWorkDir(application.getModule()));

    List<MvcPluginDescriptor> res = new ArrayList<>();

    if (sdkWorkDir == null) {
      LOG.warn("Grails/Griffon work directory not found.");
      return Collections.emptyList();
    }

    if (!sdkWorkDir.isDirectory()) {
      LOG.warn("Failed to load plugin list: Grails/Griffon work directory is not a directory (" + sdkWorkDir.getPath() + ')');
      return Collections.emptyList();
    }

    sdkWorkDir.refresh(false, false);

    boolean pluginListExists = false;

    for (VirtualFile file : sdkWorkDir.getChildren()) {
      if (isPluginListFile(file)) {
        res.addAll(getPluginList(file));
        pluginListExists = true;
      }
    }

    PLUGIN_LIST_DONOT_DOWNLOADED.set(application, pluginListExists ? null : true);

    return res;
  }

  private static boolean isPluginListFile(VirtualFile file) {
    return !file.isDirectory() && file.getName().startsWith("plugins-list") && file.getName().endsWith(".xml");
  }

  public static @NotNull List<MvcPluginDescriptor> loadPluginList(@NotNull Module module) {
    GrailsFramework framework = GrailsFramework.getInstance(module);
    if (framework == null) {
      return Collections.emptyList();
    }

    File dir = framework.getSdkWorkDir(module);
    if (dir == null) {
      return Collections.emptyList();
    }
    VirtualFile sdkWorkDir = LocalFileSystem.getInstance().findFileByIoFile(dir);

    if (sdkWorkDir == null || !sdkWorkDir.isDirectory()) {
      return Collections.emptyList();
    }

    List<MvcPluginDescriptor> res = new ArrayList<>();

    sdkWorkDir.refresh(true, false);

    for (VirtualFile file : sdkWorkDir.getChildren()) {
      if (isPluginListFile(file)) {
        res.addAll(getPluginList(file));
      }
    }

    return res;
  }

  private static Collection<MvcPluginDescriptor> parsePluginList(VirtualFile file) {
    List<MvcPluginDescriptor> res = new ArrayList<>();

    InputStream in = null;
    try {
      in = file.getInputStream();

      Document document = new SAXBuilder().build(in);

      Element root = document.getRootElement();

      for (Element pluginElement : root.getChildren("plugin")) {
        String name = pluginElement.getAttributeValue("name");
        if (name == null) {
          continue;
        }

        MvcPluginDescriptor plugin = new MvcPluginDescriptor(name);

        if (StringUtil.isNotEmpty(name)) {
          String latestVersion = pluginElement.getAttributeValue("latest-release");

          for (Element releaseElement : pluginElement.getChildren("release")) {
            MvcPluginDescriptor.Release release = new MvcPluginDescriptor.Release(
              plugin,
              releaseElement.getAttributeValue("version"),
              releaseElement.getAttributeValue("type"),
              releaseElement.getChildTextTrim("title"),
              releaseElement.getChildTextTrim("author"),
              releaseElement.getChildTextTrim("description"),
              releaseElement.getChildTextTrim("authorEmail"),
              releaseElement.getChildTextTrim("file"),
              releaseElement.getChildTextTrim("documentation"));

            plugin.getReleases().add(release);
          }

          MvcPluginDescriptor.Release latestRelease = null;

          if (StringUtil.isEmpty(latestVersion)) {
            if (!plugin.getReleases().isEmpty()) {
              latestRelease = plugin.getReleases().get(plugin.getReleases().size() - 1);
            }
          }
          else {
            for (MvcPluginDescriptor.Release release : plugin.getReleases()) {
              if (latestVersion.equals(release.getVersion())) {
                latestRelease = release;
                break;
              }
            }
          }

          plugin.setLastRelease(latestRelease);

          res.add(plugin);
        }
      }
    }
    catch (Exception e) {
      LOG.warn("Failed to read plugins file:" + file.getPath(), e);
      Notifications.Bus.notify(
        new Notification(
          GrailsUtils.GRAILS_NOTIFICATION_GROUP,
          GrailsBundle.message("mvc.plugins.notification.title.failed.to.load.plugin.list"),
          GrailsBundle.message("mvc.plugins.notification.content.failed.to.parse", file.getPath()),
          NotificationType.ERROR
        )
      );
    }
    finally {
      if (in != null) {
        try {
          in.close();
        }
        catch (IOException e) {
          LOG.error(e);
        }
      }
    }

    return res;
  }

  public static Collection<MvcPluginDescriptor> getPluginList(VirtualFile file) {
    Pair<Collection<MvcPluginDescriptor>, Long> cachedValue = file.getUserData(PLUGIN_DESCRIPTOR_KEY);
    if (cachedValue == null || cachedValue.second != file.getModificationCount()) {
      Collection<MvcPluginDescriptor> res = parsePluginList(file);

      cachedValue = Pair.create(res, file.getModificationCount());
      file.putUserData(PLUGIN_DESCRIPTOR_KEY, cachedValue);
    }

    return cachedValue.first;
  }

  public static @Nullable MvcPluginDescriptor parsePluginXml(VirtualFile file) {
    try {
      return parsePluginXml(file.getInputStream());
    }
    catch (IOException e) {
      LOG.error(e);
    }
    return null;
  }

  public static @Nullable MvcPluginDescriptor parsePluginXml(InputStream inputStream) throws IOException {
    Element root;

    try {
      Document document = new SAXBuilder().build(inputStream);
      root = document.getRootElement();
    }
    catch (JDOMException e) {
      return null;
    }

    String name = root.getAttributeValue("name");
    if (StringUtil.isEmpty(name)) {
      throw new IOException("Name of plugin cannot be null.");
    }

    String version = root.getAttributeValue("version");

    MvcPluginDescriptor mvcPlugin = new MvcPluginDescriptor(name);
    MvcPluginDescriptor.Release release = new MvcPluginDescriptor.Release(
      mvcPlugin,
      version,
      "",
      root.getChildTextTrim("title"),

      root.getChildTextTrim("author"),
      root.getChildTextTrim("description"),
      root.getChildTextTrim("authorEmail"),
      root.getChildTextTrim("file"),
      root.getChildTextTrim("documentation"));

    mvcPlugin.getReleases().add(release);
    mvcPlugin.setLastRelease(release);

    return mvcPlugin;
  }

  public static @Nullable MvcPluginDescriptor extractPluginInfo(@NotNull String path) {
    try (ZipFile zipFile = new ZipFile(path)) {
      ZipEntry entry = zipFile.getEntry("plugin.xml");
      if (entry != null) {
        return parsePluginXml(zipFile.getInputStream(entry));
      }
    }
    catch (IOException e) {
      LOG.error(path, e);
    }
    return null;
  }

  public static Map<String, String> getInstalledPluginVersions(@NotNull PropertiesFile applicationProperties) {
    Map<String, String> pluginNames = new HashMap<>();

    for (final IProperty property : applicationProperties.getProperties()) {
      String propName = property.getName();
      if (propName != null) {
        propName = propName.trim();
        if (propName.startsWith("plugins.")) {
          String pluginName = propName.substring("plugins.".length());
          String pluginVersion = property.getValue();
          if (!pluginName.isEmpty() && pluginVersion != null) {
            pluginVersion = pluginVersion.trim();
            if (!pluginVersion.isEmpty()) {
              pluginNames.put(pluginName, pluginVersion);
            }
          }
        }
      }
    }

    return pluginNames;
  }

}
