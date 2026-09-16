package com.intellij.bigdatatools.visualization.inlays.components

import java.util.EventListener

fun interface TabChangeListener : EventListener {
  fun tabChanged(name: String)
}