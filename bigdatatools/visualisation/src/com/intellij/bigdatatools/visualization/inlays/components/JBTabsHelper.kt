package com.intellij.bigdatatools.visualization.inlays.components

import com.intellij.ui.tabs.impl.JBTabsImpl
import java.lang.Integer.max

// JBTabsImpl.myHeaderFitSize is deprecated, and this is the simplest way to get the required header height of this page control.
internal fun JBTabsImpl.getHeaderHeight(): Int {
  val labelHeight: Int = getInfoToLabel().maxByOrNull { it.value.preferredSize.height }?.value?.preferredSize?.height ?: 0
  val toolbarHeight: Int = infoToToolbar.maxByOrNull { it.value.preferredSize.height }?.value?.preferredSize?.height ?: 0
  return max(labelHeight, toolbarHeight) + borderThickness
}