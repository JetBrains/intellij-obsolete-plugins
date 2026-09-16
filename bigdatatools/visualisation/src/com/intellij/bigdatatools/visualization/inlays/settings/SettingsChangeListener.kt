package com.intellij.bigdatatools.visualization.inlays.settings

import java.util.EventListener

fun interface SettingsChangeListener : EventListener {
  fun stateChanged()
}