package com.intellij.bigdatatools.zeppelin.dependency.ui

import javax.swing.Icon

interface IconProvider {
  fun getIcon(row: Int, col: Int): Icon?
}