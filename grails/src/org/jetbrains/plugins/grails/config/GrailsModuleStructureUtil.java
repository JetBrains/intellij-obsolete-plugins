// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.config;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.ThreadingAssertions;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.GroovyMvcIcons;
import org.jetbrains.plugins.grails.runner.GrailsCommandExecutorUtil;
import org.jetbrains.plugins.grails.runner.GrailsConsole;
import org.jetbrains.plugins.grails.sdk.GrailsSDK;
import org.jetbrains.plugins.grails.sdk.GrailsSDKManager;
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager;
import org.jetbrains.plugins.grails.structure.OldGrailsApplication;
import org.jetbrains.plugins.grails.structure.impl.Grails2Application;
import org.jetbrains.plugins.grails.util.version.Version;
import org.jetbrains.plugins.groovy.mvc.MvcCommand;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class GrailsModuleStructureUtil {

  private static final Logger LOG = Logger.getInstance(GrailsModuleStructureUtil.class);
  private static final @NonNls String INPLACE_PLUGINS_MODULE_SUFFIX = "-inplacePlugin";
  private static final @NonNls String INPLACE_PLUGINS_MODULE_INFIX_OLD = "-grailsPlugin-";
  public static final @NonNls String GRAILS_VERSION_KEY = "app.grails.version";
  static final @NonNls String UPGRADE_COMMAND = "upgrade";
  static final @NonNls String UPGRADE_COMMAND_2_4_x = "set-grails-version";
  static final @NonNls String YES = "y\n";
  static final @NonNls String YESx2 = YES + YES;

  private GrailsModuleStructureUtil() {
  }

  public static Set<Module> getAllCustomPluginModules(Module module) {
    final Set<Module> res = new HashSet<>();
    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(module.getProject()).getFileIndex();

    collectCustomPluginModules(module, res, fileIndex);

    res.remove(module);

    return res;
  }

  private static void collectCustomPluginModules(Module module, Set<Module> result, ProjectFileIndex fileIndex) {
    ThreadingAssertions.assertEventDispatchThread();

    if (!result.add(module)) return;

    final Map<String, VirtualFile> locations = GrailsFramework.getCustomPluginLocations(module, true);

    for (VirtualFile virtualFile : locations.values()) {
      Module candidate = fileIndex.getModuleForFile(virtualFile);
      if (candidate != null
          && Comparing.equal(fileIndex.getContentRootForFile(virtualFile), virtualFile)
          && GrailsFramework.getInstance().hasSupport(module)) {
        collectCustomPluginModules(candidate, result, fileIndex);
      }
    }
  }

  /**
   * Upgrades version from application.properties to version from grails library
   *
   * @param module Grails module
   * @param force  whether to bypass checks and force upgrade
   */
  public static void upgradeGrails(final @NotNull Module module, boolean force) {
    final OldGrailsApplication grailsApplication = GrailsApplicationManager.findApplication(module);
    if (!(grailsApplication instanceof Grails2Application)) {
      if (grailsApplication == null && force) throw new IllegalStateException("Should not get here");
      return;
    }
    // find current library version

    final GrailsSDK grailsSdk = GrailsSDKManager.getGrailsSdk(grailsApplication);
    if (grailsSdk == null) return;

    final GrailsSettings grailsSettings = GrailsSettingsService.getGrailsSettings(module);
    final Version sdkVersion = grailsSdk.getVersion();
    final Version appPropertiesVersion = ((Grails2Application)grailsApplication).getApplicationPropertiesVersion();

    if (appPropertiesVersion != null) {
      if (appPropertiesVersion.equals(sdkVersion) || !force && appPropertiesVersion.equalsToString(grailsSettings.fixedGrailsVersion)) {
        return;
      }
    }

    boolean ask = !force && appPropertiesVersion != null;

    if (ask) {
      int resultValue = Messages.showDialog(
        GrailsBundle.message("grails.malformed.version", appPropertiesVersion, sdkVersion, grailsApplication.getName()),
        GrailsBundle.message("grails.upgrade.app"),
        new String[]{
          GrailsBundle.message("button.upgrade.yes"),
          GrailsBundle.message("button.upgrade.no"),
          GrailsBundle.message("button.upgrade.ignore", appPropertiesVersion)
        },
        0,
        GroovyMvcIcons.Grails
      );
      if (resultValue != 0) {
        if (resultValue == 2) {
          grailsSettings.fixedGrailsVersion = String.valueOf(appPropertiesVersion);
        }
        return;
      }
    }

    try {
      final MvcCommand command = sdkVersion.compareTo(Version.GRAILS_2_4_0) >= 0
                                 ? new MvcCommand(UPGRADE_COMMAND_2_4_x, String.valueOf(sdkVersion))
                                 : new MvcCommand(UPGRADE_COMMAND);
      final GeneralCommandLine commandLine = GrailsCommandExecutorUtil.createCommandLine(grailsApplication, command);
      ApplicationManager.getApplication().invokeLater(
        () -> GrailsConsole.executeProcess(grailsApplication.getProject(), commandLine, null, true, YESx2)
      );
    }
    catch (ExecutionException e) {
      GrailsConsole.NOTIFICATION_GROUP.createNotification(GrailsBundle.message("failed.to.execute.grails.command"), e.getMessage(), NotificationType.ERROR);
      LOG.info(e);
    }
  }

  public static boolean isIdeaGeneratedCustomPluginModule(Module pluginModule) {
    String name = pluginModule.getName();
    return name.endsWith(INPLACE_PLUGINS_MODULE_SUFFIX) || name.contains(INPLACE_PLUGINS_MODULE_INFIX_OLD);
  }

  public static String generateInplacePluginModuleName(String pluginName) {
    return pluginName + INPLACE_PLUGINS_MODULE_SUFFIX;
  }
}
