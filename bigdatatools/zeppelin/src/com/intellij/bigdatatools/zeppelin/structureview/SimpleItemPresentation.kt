package com.intellij.bigdatatools.zeppelin.structureview

import com.intellij.navigation.ItemPresentation
import javax.swing.Icon

data class SimpleItemPresentation(val text: String?,
                                  val locationStr: String?,
                                  val usedIcon: Icon?,
                                  val unusedIcon: Icon?) : ItemPresentation {
  override fun getPresentableText(): String? = text
  override fun getLocationString(): String? = locationStr
  override fun getIcon(unused: Boolean): Icon? = if (unused) unusedIcon else usedIcon
}