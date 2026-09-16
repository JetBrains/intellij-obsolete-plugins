// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bigdatatools.zeppelin.integration

import com.intellij.bigdatatools.zeppelin.file.ZeppelinFileType
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.ide.plugins.advertiser.PluginData
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.UnknownFeature
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.UnknownFeaturesCollector
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.installAndEnable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import com.intellij.util.PlatformUtils
import java.util.function.Function
import javax.swing.JComponent

class ZeppelinPluginAdvertiserEditorNotificationProvider : EditorNotificationProvider, DumbAware {
  private val enabledPlugins = HashSet<PluginData>()
  private val supportPlugins = setOf(bashPlugin, databasePlugin, markdownPlugin) +
                               if (PlatformUtils.isIntelliJ()) listOf(scalaPlugin) else listOf()

  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?>? {
    if (file.fileType !is ZeppelinFileType) return null

    return Function {
      createPanel(project)
    }
  }

  private fun createPanel(project: Project): EditorNotificationPanel? {

    val disabledOrNotInstalledPlugins = getDisabledOrNotInstalledPlugins(project)
    if (disabledOrNotInstalledPlugins.isEmpty())
      return null

    val msg = if (disabledOrNotInstalledPlugins.size > 1) {
      ZepMessagesBundle.message("plugins.found", disabledOrNotInstalledPlugins.joinToString(separator = ", ") { it.pluginName })
    }
    else {
      ZepMessagesBundle.message("plugin.found", disabledOrNotInstalledPlugins.first().pluginName)
    }

    val panel = EditorNotificationPanel(EditorNotificationPanel.Status.Info)
    panel.text = msg
    panel.createActionLabel(ZepMessagesBundle.message("plugin.install")) {
      installPlugins(disabledOrNotInstalledPlugins)
      EditorNotifications.getInstance(project).updateAllNotifications()
    }

    panel.createActionLabel(ZepMessagesBundle.message("plugin.ignore")) {
      disabledOrNotInstalledPlugins.forEach {
        UnknownFeaturesCollector.getInstance(project).ignoreFeature(createExtensionFeature(it))
      }
      EditorNotifications.getInstance(project).updateAllNotifications()
    }
    return panel
  }

  private fun installPlugins(notInstalledPlugins: List<PluginData>) {
    val pluginIds = notInstalledPlugins.map { it.pluginId }.toMutableSet()
    installAndEnable(null, pluginIds, true) {
      enabledPlugins.addAll(notInstalledPlugins)
    }
  }

  private fun getDisabledOrNotInstalledPlugins(project: Project): List<PluginData> {
    val notIgnoredPlugins = supportPlugins.filterNot { isIgnored(project, it) }
    val (installed, notInstalled)
      = notIgnoredPlugins.partition { PluginManagerCore.getPlugin(PluginId.getId(it.pluginIdString)) != null }
    val disabledPlugins = installed.filter { PluginManagerCore.isDisabled(PluginId.getId(it.pluginIdString)) }

    return (notInstalled + disabledPlugins).distinct()
  }

  private fun isIgnored(project: Project, plugin: PluginData): Boolean =
    enabledPlugins.contains(plugin) || UnknownFeaturesCollector.getInstance(project).isIgnored(createExtensionFeature(plugin))

  companion object {
    private fun createExtensionFeature(plugin: PluginData): UnknownFeature =
      UnknownFeature("InterpreterPlugin", ZepMessagesBundle.message("plugin.feature.name"),
                     plugin.pluginIdString, plugin.pluginName)

    private val bashPlugin = PluginData("com.jetbrains.sh", "Shell Script", true)
    private val scalaPlugin = PluginData("org.intellij.scala", "Scala", false)
    private val databasePlugin = PluginData("com.intellij.database", "Database Tools and SQL", true)
    private val markdownPlugin = PluginData("org.intellij.plugins.markdown", "Markdown", true)
  }
}