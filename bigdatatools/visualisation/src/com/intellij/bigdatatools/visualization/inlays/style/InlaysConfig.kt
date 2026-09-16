package com.intellij.bigdatatools.visualization.inlays.style

import com.intellij.bigdatatools.visualization.inlays.settings.SettingsChangeListener
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil

enum class InlaysOutputLayout {
  TABS, // Each output in separate tab
  VERTICAL, // All outputs vertically
}

enum class InlaysToolbarStyle {
  FULL, // Each action - separate button on toolbar.
  SHORT // Only one visible action "..." and all actions in popup menu.
}

enum class HighlightMode {
  NONE,
  SELECTED,
  ALL
}

enum class OutputToolbarPosition {
  TOP,
  LEFT,
  RIGHT,
  BOTTOM
}

@State(name = "InlaysConfig", storages = [Storage("InlaysStyleConfig.xml")])
class InlaysConfig : PersistentStateComponent<InlaysConfig> {

  companion object {
    fun getInstance(): InlaysConfig = service()
  }

  var cellHighlightMode = HighlightMode.ALL

  var outputHighlightMode = HighlightMode.NONE

  var drawSeparatorLine = false

  /** Shows started time and username of last user running cell. */
  var showBottomInfo = true

  var inlayGutterCollapsible = true

  var outputLayout = InlaysOutputLayout.VERTICAL

  var transparentOutput = true

  var transparentGutter = true

  /** Enable/Disable cell floating toolbar which appears when cell is selected. */
  var cellToolbar = true

  var cellToolbarStyle = InlaysToolbarStyle.FULL
  var outputToolbarStyle = InlaysToolbarStyle.FULL
  var outputToolbarPosition = OutputToolbarPosition.RIGHT

  /** Enabled auto-conversion of text output from df.show() */
  var convertTextToTable = true

  /**
   *  With this flag set to true we will not convert strings more than specialDoubleConvertionLength to doubles.
   *  This was made to prevent loosing significant digits in case like "1.21412523562363462362435252".
   */
  var specialDoubleConvertion = false
  var specialDoubleConvertionLength = 16

  override fun getState() = this

  override fun loadState(state: InlaysConfig) = XmlSerializerUtil.copyBean(state, this)

  private val listeners = mutableListOf<SettingsChangeListener>()

  fun addListener(listener: SettingsChangeListener) {
    listeners += listener
  }

  fun removeListener(listener: SettingsChangeListener) {
    listeners -= listener
  }

  fun notifyChange() {
    listeners.forEach { it.stateChanged() }
  }
}